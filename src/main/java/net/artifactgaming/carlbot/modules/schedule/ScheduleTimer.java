package net.artifactgaming.carlbot.modules.schedule;

public class ScheduleTimer {

    Schedule schedule;

    private OnScheduleTimerReachedEvent onScheduleTimerReachedEvent;

    private java.util.Timer timer;

    public ScheduleTimer(Schedule schedule){

        this.schedule = schedule;

        timer = new java.util.Timer();

        timer.schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        onScheduleTimerCompleted();
                        timer.cancel();
                    }
                },
                schedule.getTimeInMilisecondsToScheduleReached()
        );
    }

    private void onScheduleTimerCompleted(){
        // TODO: PM the user about the schedule.

        if (onScheduleTimerReachedEvent != null){
            onScheduleTimerReachedEvent.onScheduleTimerReachedEvent(this);
        }
    }

    public void registerOnScheduleTimerReachedEvent(OnScheduleTimerReachedEvent onScheduleTimerReachedEvent){
        this.onScheduleTimerReachedEvent = onScheduleTimerReachedEvent;
    }

    public Schedule getSchedule(){
        return schedule;
    }

    public void cancelScheduleTimer(){
        timer.cancel();
        timer.purge();
    }
}
