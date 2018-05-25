package com.yczhang.videoreplayer;

public class Interval {
    private long start;
    private long end;

    public Interval(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public long getLength() {
        return end-start;
    }

    public String getStartString() {
        return formatTime(start);
    }

    public String getEndString() {
        return formatTime(end);
    }

    private static String formatTime(long t) {
        long rem = t%1000;
        t = t/1000;
        return String.format("%d:%d%d:%d%d.%d%d%d",
                t/3600,(t%3600)/600,((t%3600)/60)%10,(t%60)/10,t%10,
                rem/100,(rem/10)%10,rem%10);
    }
}
