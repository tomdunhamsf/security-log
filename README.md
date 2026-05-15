# Security log analyzer
Simple app to upload and peruse log files.  Can analyze log files for anomalies, highlighting affected records.
Summary for reason for flag will be given.  Production system should be trained on baseline for user environment.
For demo purposes GPT will be used. An OPEN_AI_API_KEY can be passed as an environment variable.

For demo purposes, I am monitoring http logs of a fictional pro-war web blog at www.america-yes.com.
The log format is a subset of z-scaler logs - https://help.zscaler.com/zia/nss-feed-output-format-web-logs
The format is:
time cip sip login ua method url respcode reqhdrsize reqsize resphrdsize respsize referrer


## Design trade-offs given time constraints
I expect that a BERT based transformer pretrained on real web data word be better and cheaper.
There is also a pretrained BERT based log analyzer on HuggingFace, but GPT works on the test files reasonably well.
(I do think the first log file is more accurately considered a Slowloris type attack, however.)

Given time constraints, I did not want to move to a Flask backed, as I have not used it before.  Therefore, I'm just going with
SpringBoot with Langchanin4j and my OpenAI API key.  I'd feel more comfortable using huggingface transformers in Python for
a prod system.  I strongly suspect I could just tell Claude Code to translate the Spring Boot to Flask,
but in a short time period I can evaluate the quality of a Spring Boot app better.

