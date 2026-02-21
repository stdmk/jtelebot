# jtelebot
Java Telegram Bot (Spring)

## List of commands  

### Core
/echo — chatbot (need training).  
/help — bot commands documentation.  
/getid — information on user and group IDs.  
/start — welcome message from the bot with initial information. (Also, obtaining administrator rights for the user id specified in the properties).  
/top — user statistics: the number of messages, images, karma etc. per day, month, all the time.   
/todo — todo-list.   
/where — information about the last message of the specified user.  
/set — various bot settings: news, city, aliases, tv, holidays, disable commands, talker, zodiac sign, trainings, chatgpt.
/advice — random advices.  
/newyear — remaining time until the new year.  
/alias — set aliases of bot commands.  
/karma — information and changing the karma of chat participants.  
/time — current time of users (requires city setting).  
/holidays — user-holiday information.  
/say — bot repeats what is written.  
/up — shift up list of chat messages.  
/files — cloud storage.  
/remind — reminder.  
/training — accounting of trainings, subscriptions to them and statistics.  
/me - third person text (skype or irc /me command analog).  
/delay - delayed execution of commands.  
/reactions - reaction statistic.  
/increment - counting increments.  
/http - HTTP status code description.  
/steam - Information about games on Steam.  
/calories - calorie counting.  
/write - sending messages.  

### Information
/bash — bash.org.ru site quotes.  
/news — news from rss feeds.  
/google — search in google.  
/weather — weather information.  
/wiki — search on wikipedia.  
/wolframalpha — access to Wolfram alpha system.  
/exchange — exchange rates.  
/tv — TV program.  
/horoscope — horoscope.  
/parcel — parcel tracking.  
/movie — movie information.  
/calendar — public holiday calendar.  
/word — access to Wiktionary.  

### Administration
**⚠ Security Notice**
Some commands provide direct access to:
- system console  
- database  
- file system  
- server shutdown

/level — command access level. From 0 (complete ignore) to 10 - full access.  
/sql — SQL command line with access to the bot database (use with caution, make backups (/backup)).  
/logs — bot logs.  
/cmd — access to the system console of the machine where the bot is running (use with caution).  
/uptime — bot statistics.  
/shutdown — shutdown bot.  
/backup — bot database backup (sent as a file to telegram).  
/errors — list of errors that have occurred in the bot (for administrators).  
/delete - deleting messages.

### Media
/boobs — erotic images.  
/butts — erotic images.  
/picture — image search.  
/truth — truthfulness of expressions.  
/turn — translation of text from one layout to another (ghbdtn -> привет).    
/image — search or download images via direct links and send them to chat.  
/cats — images of cats.  
/webcam — webcam video.  
/dogs — images of dogs.  

### Utilities
/password — password generation.  
/ping — resources reachability.
/webscreen — screenshots of web pages.  
/calc — calculator.  
/speller — search for errors in the text.  
/timedelta — date time delta.  
/translate — translator.  
/qr — reading and generating qr-codes.  
/timedownloading — download time calculation.  
/uuid — random uuid generation or checking.  
/download — download files via direct links and send them to chat.  
/number — telephone number information.  
/convert — unit conversion.
/metadata — metadata from file.
/location — sending location by coordinates.  
/voice — speech synthesis (and recognizing).  
/virus — checking files and links for malware.  
/length — counting the number of characters in a text or text file.  
/random - get random in range or in list.  
/latin - search for non-Latin characters.  
/json - json validation and formatting.  
/xml - xml validation and formatting.  

### AI
/chatgpt — chat with ChatGPT.  
/gigachat — chat with Gigachat.

## Properties
**telegramBotApiToken**  
You must specify the bot api token in the file properties.properties.  
To receive a token contact @BotFather.

**adminId**  
Telegram-admin user ID to gain access to all commands.  
To get the value of your ID, run the bot and use the /getid command.  
Then the admin must send the /help command to the bot for getting admin rights.  

**defaultRequestTimeoutSeconds**  
Timeout for requests to external systems' APIs  

**openweathermapId** (/weather command)  
OpenWeatherMap API access token.  
To get a token go to https://openweathermap.org/

**googleToken** (/google /image /images commands)  
Google search API access token.  
To get a token go to https://developers.google.com/custom-search/v1/overview?hl=en#prerequisites

**googleTranslateToken** (/translate command)  
Google Translator API access token.  
To get a token: write and publish the script on https://script.google.com/ and copy value after https://script.google.com/macros/s/  
```
var mock = {
  parameter:{
    q:'hello',
    source:'en',
    target:'ru'
  }
};


function doGet(e) {
  e = e || mock;

  var sourceText = ''
  if (e.parameter.q){
    sourceText = e.parameter.q;
  }

  var sourceLang = '';
  if (e.parameter.source){
    sourceLang = e.parameter.source;
  }

  var targetLang = 'en';
  if (e.parameter.target){
    targetLang = e.parameter.target;
  }

  var translatedText = LanguageApp.translate(sourceText, sourceLang, targetLang, {contentType: 'html'});

  return ContentService.createTextOutput('{"text":"' + translatedText + '"}').setMimeType(ContentService.MimeType.JSON);
}
```

**screenshotMachineToken** (/screen command)  
Screenshotmachine API access token.  
To get a token go to https://www.screenshotmachine.com/

**wolframAlphaToken** (/wolframalpha command)  
WolframAlphaToken API access token.
To get a token go to https://products.wolframalpha.com/api

**kinopoiskToken** (/kinopoisk command)  
Kinopoisk.dev API access token.
To get a token go to @kinopoiskdev_bot.

**chatGptApiUrl** (/chatgpt command)  
API URL of ChatGPT.  
If the value is empty, the official one will be used: https://api.openai.com/v1/

**chatGptToken** (/chatgpt command)  
ChatGPT API access token.
To get a token go to https://openai.com/product (third party services are also suitable)

**chatGPTTokensSize** (/chatgpt command)
Tokens size limit per request.  
by default — 0 — no limit

**spyMode**  
The bot will notify you by adminId when receiving personal messages if value `true`.

**russianPostLogin** and **russianPostPassword** (/parcel command)  
Russian Post tracking access token.  
To get a credentials go to https://tracking.pochta.ru/support/faq/how_to_get_access

**russianPostRequestsLimit** (/parcel command)  
Limit of API requests. 100 is the free account limit.

**chatGPTContextSize** (/chatgpt command)  
Size of communication history with chatgpt, messages.  
16 by default. Reduce if problems arise.

**defaultLanguage**  
Default language code for the bot. Currently supported: `en`, `ru`. Default `en`.

**xmlTvFileUrl** (/tv command)  
URL to a TV program file in XMLTV (gz) format. For /tv command.

**sberApiRequestTimeoutSeconds** (/voice and /gigachat commands)  
Waiting time for response from sber API.  

**saluteSpeechSecret** (/voice command)  
Secret for access for Salute Speech services (synthesis and recognize speech).  
To get a secret go to: https://developers.sber.ru/docs/ru/salutespeech/integration

**gigaChatSecret** (/gigachat command)  
Gigachat API access secret.
To get a secret go to: https://developers.sber.ru/studio/workspaces/my-space/get/gigachat-api

**virusTotalApiKey** (/virus command)  
Virus Total API key  
To get a key go to: https://www.virustotal.com/

**ftpBackupUrl** (ftp backup)  
Url of the ftp server where backups will be uploaded  
`ftp://username:password@host:port[/path]`  

**daysBeforeExpirationBackup** (ftp backup)  
The number of days to keep the backup. Outdated backups will be deleted.  

**maxBackupsSizeBytes** (ftp backup)    
The maximum size (bytes) of stored backups. If the limit is exceeded, older backups will be deleted.  

**ftpRetryCount** (ftp backup)  
Number of FTP backup attempts.  

**ftpRetryTimeoutMillis**  
Time between backup attempts.  

**reactions** (/reactions command)

*minRepliesToGetTheTop* 
Minimum message replies required to get to the /reactions command.

*messagesCountInTheTop*
Messages count in /reactions command.

*messageExpirationDays*
number of days to store messages

## System
Java 21 is required to run the bot.
To use the /webcam command, you need to install ffmpeg on your system.

## Build
To build the JAR file:  
`gradlew build -x test`  
(The file will be located in the build/libs/ folder)  

## Run

To run without the JAR file:
1. Fill in the `TelegramBotApiToken` parameter in `properties.properties`.  
2. Run:  
   `gradlew bootrun`  

To run with the JAR file:
1. Fill in the `TelegramBotApiToken` parameter in `properties.properties`.  
2. Place `properties.properties` in the same folder as the JAR file.  
3. Run:  
   `java -jar Bot.jar`  

### Admin rights
To get admin rights:  
1. Fill in the `adminId` parameter in `properties.properties` (you can get it using the /getid command).  
2. Send `/help` to the bot.
