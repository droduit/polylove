* The server should respect the preferences of people when matching : Lucie and Simon
* The user can send and receive messages (finish) : Thierry and Tim
  * Messages sent should be stored in a local cache
  * The server receive the message:
    * It should store the message in a DB
    * It should send a notification to the receiver of the message
  * The user should get a notification when he received a new message
  * Messages received should be stored in a local cache
* Confirm or close matches (finish) : Dominique and Christophe
  * The user should say if he like his match or not during the day 
  * The user should receive a notification when he is matched
  * The chat should stay open if both users liked each other and closed else
* The server must accept like status until a certain time and must reject them if sent too late (1 week)
* Chats should be displayed by categories (today's chat, open, closed) (1 week)
* The profile of matches should be cached and expires after a while (1 week)
* The location of the user is periodically sent to the server (6 weeks)
* The server should notify a couple if they are near each other (3 weeks)
* The server should periodically reveal information from the profile of the matched user (3 weeks)
* The user should be able to fetch chats and messages that have been erased from the cache (4 weeks)
* The server should provide an API to fetch old chats and messages (3 weeks)
* The user should have enough possibility to customize his avatar (3 weeks)
* The user should have some goofy options for his profile (3 weeks)
