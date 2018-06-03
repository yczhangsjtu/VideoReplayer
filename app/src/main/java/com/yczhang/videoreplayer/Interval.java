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

    public static String formatTime(long t) {
        long rem = t%1000;
        t = t/1000;
        return String.format("%d:%d%d:%d%d.%d",
                t/3600,(t%3600)/600,((t%3600)/60)%10,(t%60)/10,t%10,
                rem/100);
    }

    @Override
    public String toString() {
        return start+","+end;
    }

    public static Interval fromString(String s) throws Exception {
        int i = s.indexOf(',');
        if(i < 0) throw new Exception("Invalid interval string");
        long a = Long.parseLong(s.substring(0,i));
        long b = Long.parseLong(s.substring(i+1));
        if(a > b) throw new Exception("Invalid interval: start > end");
        return new Interval(a,b);
    }
}
