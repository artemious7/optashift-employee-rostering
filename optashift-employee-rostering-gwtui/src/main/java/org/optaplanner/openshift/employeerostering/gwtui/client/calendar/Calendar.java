package org.optaplanner.openshift.employeerostering.gwtui.client.calendar;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import elemental2.dom.MouseEvent;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Panel;
import org.gwtbootstrap3.client.ui.html.Div;
import org.jboss.errai.ioc.client.container.SyncBeanManager;
import org.jboss.errai.ui.client.local.spi.TranslationService;
import org.optaplanner.openshift.employeerostering.gwtui.client.calendar.twodayview.TwoDayViewPresenter;
import org.optaplanner.openshift.employeerostering.gwtui.client.common.ConstantFetchable;
import org.optaplanner.openshift.employeerostering.gwtui.client.interfaces.DataProvider;
import org.optaplanner.openshift.employeerostering.gwtui.client.interfaces.Fetchable;
import org.optaplanner.openshift.employeerostering.gwtui.client.interfaces.HasTimeslot;
import org.optaplanner.openshift.employeerostering.gwtui.client.popups.ErrorPopup;

public class Calendar<G extends HasTitle, I extends HasTimeslot<G>> {

    CalendarPresenter<G, I> view;
    Map<I, I> shifts;
    Integer tenantId;
    SyncBeanManager beanManager;
    Fetchable<Collection<I>> dataProvider;
    Fetchable<List<G>> groupProvider;
    DataProvider<G, I> instanceCreator;
    Timer timer;
    boolean didTenantChange;

    private Calendar(Integer tenantId, Fetchable<Collection<I>> dataProvider, Fetchable<List<G>> groupProvider,
            DataProvider<G,
                    I> instanceCreator, SyncBeanManager beanManager) {
        this.beanManager = beanManager;
        this.tenantId = tenantId;

        shifts = new HashMap<>();
        didTenantChange = true;

        setInstanceCreator(instanceCreator);
        setGroupProvider(groupProvider);
        setDataProvider(dataProvider);

        timer = new Timer() {

            @Override
            public void run() {
                forceUpdate();
            }
        };

        refresh();

    }

    private void setView(CalendarPresenter<G, I> view) {
        this.view = view;
    }

    private CalendarPresenter<G, I> getView() {
        return view;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
        didTenantChange = true;
        groupProvider.fetchData(() -> dataProvider.fetchData(() -> draw()));
    }

    public void draw() {
        refresh();
        view.draw();
    }

    public void refresh() {
    }

    public void forceUpdate() {
        dataProvider.fetchData(() -> draw());
    }

    public void setDate(LocalDateTime date) {
        view.setDate(date);
        timer.schedule(1000);
    }

    public LocalDateTime getViewStartDate() {
        return view.getViewStartDate();
    }

    public LocalDateTime getViewEndDate() {
        return view.getViewEndDate();
    }

    public Collection<I> getShifts() {
        return shifts.keySet();
    }

    public Collection<G> getGroups() {
        return view.getGroups();
    }

    public Collection<G> getVisibleGroups() {
        return view.getVisibleGroups();
    }

    public void addShift(I shift) {
        shifts.put(shift, shift);
        getView().addShift(shift);
    }

    //returns original shift; add it if it doesn't exist
    public I updateShift(I newShift) {
        I oldShift = shifts.get(newShift);
        if (oldShift != null) {
            shifts.put(oldShift, newShift);
            getView().updateShift(oldShift, newShift);
            return oldShift;
        } else {
            addShift(newShift);
            return null;
        }
    }

    public void removeShift(I shift) {
        shifts.remove(shift);
        getView().removeShift(shift);
    }

    public void setDataProvider(Fetchable<Collection<I>> dataProvider) {
        if (null == dataProvider) {
            dataProvider = new ConstantFetchable<>(Collections.emptyList());
        }
        this.dataProvider = dataProvider;
        shifts.clear();
        dataProvider.setUpdatable((d) -> {
            groupProvider.fetchData(() -> {
                if (didTenantChange) {
                    shifts.clear();
                    d.forEach((e) -> shifts.put(e, e));
                    view.setShifts(getShifts());
                    view.setDate(view.getHardStartDateBound());
                    didTenantChange = false;
                } else {
                    d.forEach((e) -> updateShift(e));
                }
            });
        });
    }

    public void setGroupProvider(Fetchable<List<G>> groupProvider) {
        if (null == groupProvider) {
            groupProvider = new ConstantFetchable<>(Collections.emptyList());
        }
        this.groupProvider = groupProvider;
        groupProvider.setUpdatable((groups) -> {
            didTenantChange = true;
            getView().setGroups(groups);
        });
    }

    public void setInstanceCreator(DataProvider<G, I> instanceCreator) {
        if (null == instanceCreator) {
            instanceCreator = (c, g, s, e) -> {
            };
        }
        this.instanceCreator = instanceCreator;
    }

    public void addShift(G group, LocalDateTime start, LocalDateTime end) {
        instanceCreator.getInstance(this, group, start, end);
    }

    public LocalDateTime getHardStartDateBound() {
        return view.getHardStartDateBound();
    }

    public void setHardStartDateBound(LocalDateTime hardStartDateBound) {
        view.setHardStartDateBound(hardStartDateBound);
    }

    public LocalDateTime getHardEndDateBound() {
        return view.getHardEndDateBound();
    }

    public void setHardEndDateBound(LocalDateTime hardEndDateBound) {
        view.setHardEndDateBound(hardEndDateBound);
    }

    public int getDaysShown() {
        return view.getDaysShown();
    }

    public void setDaysShown(int daysShown) {
        view.setDaysShown(daysShown);
    }

    public int getEditMinuteGradality() {
        return view.getEditMinuteGradality();
    }

    public void setEditMinuteGradality(int editMinuteGradality) {
        view.setEditMinuteGradality(editMinuteGradality);
    }

    public int getDisplayMinuteGradality() {
        return view.getDisplayMinuteGradality();
    }

    public void setDisplayMinuteGradality(int displayMinuteGradality) {
        view.setDisplayMinuteGradality(displayMinuteGradality);
    }

    public void setViewSize(double screenWidth, double screenHeight) {
        view.setViewSize(screenWidth, screenHeight);
    }

    public <T> T getInstanceOf(Class<T> clazz) {
        return beanManager.lookupBean(clazz).newInstance();
    }

    public SyncBeanManager getBeanManager() {
        return beanManager;
    }

    public static class Builder<G extends HasTitle, T extends HasTimeslot<G>, D extends TimeRowDrawable<G, T>> {

        Panel container;
        Collection<T> shifts;
        Integer tenantId;
        LocalDateTime startAt;
        Fetchable<Collection<T>> dataProvider;
        Fetchable<List<G>> groupProvider;
        DataProvider<G, T> instanceCreator;
        SyncBeanManager beanManager;
        TranslationService translator;
        DateDisplay dateDisplay;

        public Builder(Panel container, Integer tenantId, TranslationService translator) {
            this.container = container;
            this.tenantId = tenantId;
            this.translator = translator;

            dataProvider = null;
            groupProvider = null;
            instanceCreator = null;
            startAt = null;
            dateDisplay = DateDisplay.WEEK_STARTING;
        }

        public Builder<G, T, D> fetchingDataFrom(Fetchable<Collection<T>> dataProvider) {
            this.dataProvider = dataProvider;
            return this;
        }

        public Builder<G, T, D> fetchingGroupsFrom(Fetchable<List<G>> groupProvider) {
            this.groupProvider = groupProvider;
            return this;
        }

        public Builder<G, T, D> creatingDataInstancesWith(DataProvider<G, T> instanceCreator) {
            this.instanceCreator = instanceCreator;
            return this;
        }

        public Builder<G, T, D> displayWeekAs(DateDisplay dateDisplay) {
            this.dateDisplay = dateDisplay;
            return this;
        }

        public Builder<G, T, D> startingAt(LocalDateTime start) {
            startAt = start;
            return this;
        }

        public Builder<G, T, D> withBeanManager(SyncBeanManager beanManager) {
            this.beanManager = beanManager;
            return this;
        }

        public Calendar<G, T> asTwoDayView(TimeRowDrawableProvider<G, T, D> drawableProvider) {
            if (null != beanManager) {
                Calendar<G, T> calendar = new Calendar<>(tenantId,
                        dataProvider,
                        groupProvider, instanceCreator, beanManager);
                TwoDayViewPresenter<G, T, D> view = new TwoDayViewPresenter<G, T, D>(calendar,
                        drawableProvider, dateDisplay, translator);
                calendar.setView(view);

                if (null != startAt) {
                    view.setDate(startAt);
                }
                Div tmp = new Div();
                tmp.getElement().appendChild((com.google.gwt.dom.client.Element) view.getElement());
                container.add(tmp);
                calendar.setViewSize(container.getOffsetWidth(), container.getOffsetHeight());
                return calendar;
            } else {
                throw new IllegalStateException("You must set all of "
                        + "(beanManager) before calling this method.");
            }
        }

    }
}
