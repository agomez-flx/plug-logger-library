package com.plug.logger;

import java.util.ArrayList;
import java.util.List;

public class LogEntry {
    private static final String FILED_SEPARATOR = "|";
    private String className;
    private String method;
    private List<String> parameters;
    private String errorCode;
    private long duration;

    public LogEntry() {
        parameters = new ArrayList<>();
    }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public List<String> getParameters() { return parameters; }
    public void setParameters(List<String> parameters) { this.parameters = parameters; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    @Override
    public String toString() {
        String separator = FILED_SEPARATOR;
        StringBuilder sb = new StringBuilder("LogEntry");
        sb.append(separator).append("---------------------------").append(separator);
        sb.append("ClassName: ").append(className).append(separator);
        sb.append("Method: ").append(method).append(separator);
        sb.append("Parameters: ");
        for (String string : parameters) {
            sb.append(string).append(",");
        }
        sb.append(separator);
        sb.append("ErrorCode: ").append(errorCode).append(separator);
        sb.append("Duration: ").append(duration).append("ms").append(separator);
        sb.append("---------------------------");
        return sb.toString();
    }
}