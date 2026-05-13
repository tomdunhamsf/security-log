package com.securitylog.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "logs", indexes = {
    @Index(name = "idx_logs_log_name", columnList = "log_name")
})
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "log_name", nullable = false)
    private String logName;

    @Column(name = "time")
    private String time;

    @Column(name = "cip")
    private String cip;

    @Column(name = "sip")
    private String sip;

    @Column(name = "login")
    private String login;

    @Column(name = "ua", length = 1024)
    private String ua;

    @Column(name = "method", length = 16)
    private String method;

    @Column(name = "url", length = 2048)
    private String url;

    @Column(name = "respcode", length = 8)
    private String respcode;

    @Column(name = "reqhdrsize")
    private String reqhdrsize;

    @Column(name = "reqsize")
    private String reqsize;

    // Note: column name matches the spec exactly (resphrdsize, not resphdrsize)
    @Column(name = "resphrdsize")
    private String resphrdsize;

    @Column(name = "respsize")
    private String respsize;

    @Column(name = "referrer", length = 2048)
    private String referrer;

    public Long getRecordId() { return recordId; }

    public String getLogName() { return logName; }
    public void setLogName(String logName) { this.logName = logName; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getCip() { return cip; }
    public void setCip(String cip) { this.cip = cip; }

    public String getSip() { return sip; }
    public void setSip(String sip) { this.sip = sip; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getUa() { return ua; }
    public void setUa(String ua) { this.ua = ua; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getRespcode() { return respcode; }
    public void setRespcode(String respcode) { this.respcode = respcode; }

    public String getReqhdrsize() { return reqhdrsize; }
    public void setReqhdrsize(String reqhdrsize) { this.reqhdrsize = reqhdrsize; }

    public String getReqsize() { return reqsize; }
    public void setReqsize(String reqsize) { this.reqsize = reqsize; }

    public String getResphrdsize() { return resphrdsize; }
    public void setResphrdsize(String resphrdsize) { this.resphrdsize = resphrdsize; }

    public String getRespsize() { return respsize; }
    public void setRespsize(String respsize) { this.respsize = respsize; }

    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }
}
