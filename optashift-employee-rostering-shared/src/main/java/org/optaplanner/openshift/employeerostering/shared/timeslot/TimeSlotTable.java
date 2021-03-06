package org.optaplanner.openshift.employeerostering.shared.timeslot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TimeSlotTable<T> {

    /**
     * List of interval start points, in sorted order
     */
    List<BoundaryPoint> startPoints;

    /**
     * List of interval end points, in sorted order
     */
    List<BoundaryPoint> endPoints;

    /**
     * Holds data belonging to an interval
     */
    Map<UUID, TimeSlot<T>> intervalData;

    public TimeSlotTable() {
        startPoints = new ArrayList<>();
        endPoints = new ArrayList<>();
        intervalData = new HashMap<>();
    }

    private int getFirstIndexOf(int index, BoundaryPoint o) {
        if (index < 0) {
            return -(index + 1);
        }
        List<BoundaryPoint> intervalPoints = (o.isStartPoint()) ? startPoints : endPoints;

        while (index > 0 && intervalPoints.get(index - 1).equals(o)) {
            index--;
        }
        return index;
    }

    private int getLastIndexOf(int index, BoundaryPoint o) {
        if (index < 0) {
            return -(index + 1);
        }

        List<BoundaryPoint> intervalPoints = (o.isStartPoint()) ? startPoints : endPoints;
        while (index < intervalPoints.size() - 1 && intervalPoints.get(index + 1).equals(o)) {
            index++;
        }
        return index;
    }

    public UUID add(long start, long end, T data) {
        UUID uuid = UUID.randomUUID();
        BoundaryPoint startPoint = new BoundaryPoint(start, true, uuid);
        BoundaryPoint endPoint = new BoundaryPoint(end, false, uuid);

        int insertionPoint = getFirstIndexOf(Collections.binarySearch(startPoints, startPoint), startPoint);
        startPoints.add(insertionPoint, startPoint);
        insertionPoint = getLastIndexOf(Collections.binarySearch(endPoints, endPoint), endPoint);
        endPoints.add(insertionPoint, endPoint);

        intervalData.put(uuid, new TimeSlot<T>(startPoint, endPoint, data));
        return uuid;
    }

    public UUID get(T data) {
        for (Entry<UUID, TimeSlot<T>> e : intervalData.entrySet()) {
            if (e.getValue().getData().equals(data)) {
                return e.getKey();
            }
        }
        return null;
    }

    public void update(UUID uuid, T newData) {
        TimeSlot<T> orig = intervalData.get(uuid);
        intervalData.put(uuid, new TimeSlot<>(orig.getStartPoint(), orig.getEndPoint(), newData));
    }

    public void remove(TimeSlot<T> timeSlot) {
        BoundaryPoint startPoint = timeSlot.getStartPoint();
        BoundaryPoint endPoint = timeSlot.getEndPoint();

        int startIndex = getLastIndexOf(Collections.binarySearch(startPoints, startPoint), startPoint);
        int endIndex = getLastIndexOf(Collections.binarySearch(endPoints, endPoint), endPoint);

        while (!startPoints.get(startIndex).getUUID().equals(startPoint.getUUID())) {
            startIndex--;
        }

        while (!endPoints.get(endIndex).getUUID().equals(endPoint.getUUID())) {
            endIndex--;
        }

        intervalData.remove(timeSlot.getUUID());
        startPoints.remove(startIndex);
        endPoints.remove(endIndex);
    }

    public void remove(UUID uuid) {
        if (!intervalData.keySet().contains(uuid)) {
            StringBuilder errorMsg = new StringBuilder("UUID \"" + uuid + "\" was not found:\nUUIDS: {");
            intervalData.keySet().forEach((e) -> errorMsg.append(e.toString() + ";"));
            errorMsg.append("}");
            throw new RuntimeException(errorMsg.toString());
        }
        remove(intervalData.get(uuid));
    }

    public UUID remove(long start, long end) {
        BoundaryPoint startPoint = new BoundaryPoint(start, true, null);
        BoundaryPoint endPoint = new BoundaryPoint(end, false, null);

        int startIndex = getLastIndexOf(Collections.binarySearch(startPoints, startPoint), startPoint);
        final int endIndex = getLastIndexOf(Collections.binarySearch(endPoints, endPoint), endPoint);

        int pairIndex = endIndex;

        UUID uuid = startPoints.get(startIndex).getUUID();

        // Linearly searches the end points for one with the same UUID as the start point,
        // If none can be found, the next start point is checked
        while (!startPoints.get(startIndex).getUUID().equals(endPoints.get(pairIndex).getUUID())) {
            uuid = startPoints.get(startIndex).getUUID();
            boolean found = false;
            while (pairIndex >= 0 && endPoints.get(pairIndex).equals(endPoint)) {
                if (endPoints.get(pairIndex).getUUID().equals(uuid)) {
                    found = true;
                    break;
                }
                pairIndex--;
            }
            if (found) {
                break;
            }
            pairIndex = endIndex;
            startIndex--;

        }

        uuid = startPoints.get(startIndex).getUUID();
        intervalData.remove(uuid);
        startPoints.remove(startIndex);
        endPoints.remove(pairIndex);

        return uuid;
    }

    public List<List<TimeSlot<T>>> getTimeSlotsAsGrid() {
        return new TimeSlotIterator<T>(startPoints, intervalData).getTimeSlotsAsGrid();
    }

    public List<List<TimeSlot<T>>> getTimeSlotsAsGrid(long start, long end) {
        BoundaryPoint startPoint = new BoundaryPoint(end, true, null);
        BoundaryPoint endPoint = new BoundaryPoint(start, false, null);
        int indexOfFirstEndPointAfterStart = getFirstIndexOf(Collections.binarySearch(endPoints, endPoint), endPoint);
        int indexOfLastStartPointBeforeEnd = getLastIndexOf(Collections.binarySearch(startPoints, startPoint),
                startPoint);

        Set<UUID> endPointsAfterStartUUID = endPoints.subList(indexOfFirstEndPointAfterStart, endPoints.size())
                .stream().map((e) -> e.getUUID()).collect(Collectors.toSet());
        List<BoundaryPoint> startPointsBeforeEnd = startPoints.subList(0, indexOfLastStartPointBeforeEnd).stream()
                .filter((s) -> endPointsAfterStartUUID.contains(s.getUUID())).collect(Collectors.toList());
        return new TimeSlotIterator<T>(startPointsBeforeEnd, intervalData).getTimeSlotsAsGrid();
    }

    private static final class TimeSlotIterator<T> implements Iterator<TimeSlot<T>> {

        List<BoundaryPoint> startPoints;
        Map<UUID, TimeSlot<T>> intervalData;

        int index;
        int nextDepth;

        List<TimeSlot<T>> prev;
        TimeSlot<T> next;

        public TimeSlotIterator(List<BoundaryPoint> startPoints, Map<UUID, TimeSlot<T>> intervalData) {
            this.startPoints = startPoints;
            this.intervalData = intervalData;

            index = 0;
            nextDepth = 0;
            prev = new ArrayList<>();
            next();
        }

        public List<List<TimeSlot<T>>> getTimeSlotsAsGrid() {
            List<List<TimeSlot<T>>> out = new ArrayList<>();
            while (hasNext()) {
                int depth = nextDepth;
                while (out.size() <= depth) {
                    out.add(new ArrayList<>());
                }

                TimeSlot<T> timeSlot = next();
                out.get(depth).add(timeSlot);
            }
            return out;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public TimeSlot<T> next() {
            TimeSlot<T> out = next;
            if (index < startPoints.size()) {
                BoundaryPoint startPoint = startPoints.get(index);
                next = intervalData.get(startPoint.getUUID());
                BoundaryPoint endPoint = next.getEndPoint();

                int depth;

                for (depth = 0; depth < prev.size() &&
                        startPoint.getPosition() < prev.get(depth).getEndPoint().getPosition() &&
                        endPoint.getPosition() > prev.get(depth).getStartPoint()
                                .getPosition(); depth++) {
                }

                nextDepth = depth;
                if (prev.size() <= depth) {
                    prev.add(next);
                } else {
                    prev.set(depth, next);
                }
                index++;
            } else {
                next = null;
            }
            return out;
        }
    }

    public static final class TimeSlot<T> {

        final BoundaryPoint startPoint;
        final BoundaryPoint endPoint;
        final T data;

        public TimeSlot(BoundaryPoint start, BoundaryPoint end, T data) {
            this.startPoint = start;
            this.endPoint = end;
            this.data = data;
        }

        public BoundaryPoint getStartPoint() {
            return startPoint;
        }

        public BoundaryPoint getEndPoint() {
            return endPoint;
        }

        public long getLength() {
            return endPoint.getPosition() - startPoint.getPosition();
        }

        public UUID getUUID() {
            return startPoint.getUUID();
        }

        public T getData() {
            return data;
        }

        @Override
        public int hashCode() {
            return getUUID().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof TimeSlot))
                return false;
            TimeSlot other = (TimeSlot) obj;

            return this.getUUID().equals(other.getUUID());
        }

    }

    public static final class BoundaryPoint implements Comparable<BoundaryPoint> {

        final long position;
        final boolean isStartOfBoundary;
        final UUID uuid;

        public BoundaryPoint(long pos, boolean isStart, UUID uuid) {
            this.position = pos;
            this.isStartOfBoundary = isStart;
            this.uuid = uuid;
        }

        public boolean isStartPoint() {
            return isStartOfBoundary;
        }

        public boolean isEndPoint() {
            return !isStartOfBoundary;
        }

        public long getPosition() {
            return position;
        }

        public UUID getUUID() {
            return uuid;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof BoundaryPoint) {
                BoundaryPoint other = (BoundaryPoint) o;
                return this.position == other.position && this.isStartOfBoundary == other.isStartOfBoundary;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(position) ^ Boolean.hashCode(isStartOfBoundary);
        }

        @Override
        public int compareTo(BoundaryPoint o) {
            int compareToResult = Long.compare(this.position, o.position);
            if (compareToResult != 0) {
                return compareToResult;
            }

            if (this.isStartOfBoundary == o.isStartOfBoundary) {
                return 0;
            } else if (this.isStartOfBoundary) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}