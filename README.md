Simple app to upload and peruse log files.  Can analyze log files for anomalies, highlighting affected records.
Summary for reason for flag will be given.  Production system should be trained on baseline for user environment.
For demo purposes a pretrained BERT transformer and possibly LLM will be used depending on performance.

For demo purposes, I am monitoring http logs of a fictional pro-war web blog at www.america-yes.com.
The log format is a subset of z-scaler logs - https://help.zscaler.com/zia/nss-feed-output-format-web-logs
The format is:
time cip sip login ua method url respcode reqhdrsize reqsize resphrdsize respsize referrer
