# Security log analyzer
Simple app to upload and peruse log files.  Can analyze log files for anomalies, highlighting affected records.
Summary for reason for flag will be given.  Production system should be trained on baseline for user environment.
For demo purposes GPT will be used. An OPEN_AI_KEY can be passed as an environment variable.

For demo purposes, I am monitoring http logs of a fictional pro-war web blog at www.america-yes.com.
The log format is a subset of z-scaler logs - https://help.zscaler.com/zia/nss-feed-output-format-web-logs
The format is:
time cip sip login ua method url respcode reqhdrsize reqsize resphrdsize respsize referrer

## Deploy and run
### Prerequisites
Check out the project.  
Then create a file called .env in the root directory. 
You must have a variable called OPEN_AI_KEY here.  If you have no money on this key, analysis won't work but the app otherwise runs.
You must have Docker, and start it up.
### Running
From a shell type

docker compose up --build

On initial run this will be pulling in ngnix, next.js, Spring Boot, postgresql, so the first time is slow.
The database is always cleaned between startups, so that we don't need to consider log overwrites or deletion.

### Use
The server will be at https://localhost/ - accept the self-signed cer

The login is secadmin ChangeMe123!

For this demo you can't add users or change passwords.

The three log files in the root directory can be loaded in.
america2-access.log has no anomally, the others do.
If you don't have an OpenAIAPI key, I'll give some screenshots of analysis.

Your auth cookie should be good across restarts.

The AI picks up on anomalies, but it could do better on selecting the relevant records.  See design trade-off section for how this could be improved.

## General overview
ngnix is providing ssl for the next.js in docker.
I haven't used next.js before, but this was a constraint given.
So my use might not be best practice, but basically you are going to have a page.tsx in each directory
providing your html, css.  The api directory contains various route..ts to control POSTs to the backend.
For more detail, and context used by Claude check [font.md](front.md)
Spring Boot on Tomcat is the backend.  I explain why in the tradeoffs section, but I think I would move to Flask.
Spring is a dependecy injection framework, your entry points are the controllers.
Annotations wire the rest together.  JPA handles persistence of objects.
Business logic tends to live in service by convention.
[back.md](back.md) contains context for Claude and file layout

## Design trade-offs given time constraints
I expect that a BERT based transformer pretrained on real web data word be better and cheaper.
There is also a pretrained BERT based log analyzer on HuggingFace, but GPT works on the test files reasonably well.
(I do think the first log file is more accurately considered a Slowloris type attack, however.)

Given time constraints, I did not want to move to a Flask backed, as I have not used it before.  Therefore, I'm just going with
SpringBoot with Langchanin4j and my OpenAI API key.  I'd feel more comfortable using huggingface transformers in Python for
a prod system.  I strongly suspect I could just tell Claude Code to translate the Spring Boot to Flask,
but in a short time period I can evaluate the quality of a Spring Boot app better.
Also if Claude Code chokes I can build a SpringBoot app much more quickly.
 Obviously, more tests should be written.
