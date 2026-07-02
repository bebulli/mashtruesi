================================================================
  MASHTRUESI  -  Albanian "Imposter" party game
  CS 305 Advanced Java - Semester Project
================================================================

WHAT IT IS
----------
A pass-the-phone party game (4+ players). Everyone secretly gets the
same word except the imposter(s), who instead get an optional hint.
Players take turns describing the word; the group then votes on who the
imposter is. The whole UI is in Albanian; all code, comments and Javadoc
are in English.

Headline feature: the imposter ROLE is chosen fairly (uniform), but the
speaking ORDER is biased so the imposter is LESS likely to be picked to
speak first (which would otherwise give away too much and lead to very
short rounds). This is implemented with a generic weighted random picker
and a Strategy pattern (see WeightedFirstPlayerStrategy).


REQUIREMENTS
------------
- Java 17 or newer (JDK, not just JRE)
- Maven 3.6+ with internet access (it downloads Spring Boot from Maven
  Central on first build)
- A modern web browser


HOW TO BUILD & RUN
------------------
From the project root (the folder containing pom.xml):

    mvn spring-boot:run

Then open:

    http://localhost:8080

To build a runnable jar instead:

    mvn clean package
    java -jar target/mashtruesi-1.0.0.jar

The database is H2 in-memory and is seeded automatically with Albanian
categories and words on first start. To inspect it, open:

    http://localhost:8080/h2
    JDBC URL:  jdbc:h2:mem:mashtruesi
    User:      sa     (no password)


HOW TO PLAY
-----------
1. Add at least 4 player names.
2. Pick a category (Kafshe, Ushqime, Sporte, Vende, Profesione, ...).
3. Optional: set imposter count (auto = 1, or 2 when 8+ players), turn
   the hint on/off, and adjust how rarely the imposter speaks first.
4. Pass the phone around. Each player privately taps to see their word
   (or that they are the imposter, with a hint).
5. The app announces who speaks first. Discuss, then end the round and
   record whether the group caught the imposter.


FEATURES
--------
- Round timer: optional countdown (1/2/3/5 min) shown during the round;
  when it hits zero the app automatically prompts to end the round.
- Custom words: "Shto fjale te re ne kategori" on the setup screen posts
  to /api/words and refreshes the category list.
- Category preview: word count shown per category in the dropdown and
  as a short text below it ("Kjo kategori ka N fjale.").
- Scoreboard: wins per player are tallied client-side across rounds
  played in the same session and shown on the result screen.


REST API (R8)
-------------
  POST   /api/games                      create a round
  GET    /api/games/{id}/reveal/{pos}    reveal one player's secret
  POST   /api/games/{id}/end             end round, reveal imposter+word
  GET    /api/categories                 list categories + word counts
  POST   /api/words                      add a custom word (JPA create)
  GET    /api/stats                      live + historical statistics
  DELETE /api/stats                      clear recorded history


WHERE EACH REQUIREMENT LIVES
----------------------------
R1  Collections (3+ types)
      - ConcurrentHashMap of active sessions ... service/GameService.java
      - ArrayList for assignments/order ....... service/GameService.java,
                                                 strategy/*.java
      - HashSet for imposter index lookup ..... service/GameService.java
      - TreeSet for sorted-distinct ........... util/CollectionUtils.java

R2  Generics
      - Generic class WeightedRandomPicker<T> . util/WeightedRandomPicker.java
      - Bounded generic method
        <T extends Comparable<T>> sortedDistinct util/CollectionUtils.java

R3  Lambdas & functional interfaces (2+)
      - weight function, predicates, suppliers  util/WeightedRandomPicker.java,
                                                 service/WordService.java,
                                                 service/GameService.java
      - method references (Category::getName,
        PlayerAssignment::isImposter, etc) ..... service/GameService.java,
                                                 service/WordService.java,
                                                 web/CategoryController.java

R4  Stream API (intermediate + terminal)
      - map/collect/groupingBy/counting/filter  service/WordService.java,
                                                 service/GameService.java,
                                                 web/CategoryController.java

R5  Concurrency
      - ExecutorService + CompletableFuture
        (supplyAsync/thenCombine/join) ......... service/GameService.java,
                                                 web/StatsController.java
      - ConcurrentHashMap, AtomicLong,
        AtomicInteger, volatile, synchronized .. service/GameService.java,
                                                 model/GameSession.java
      - @PreDestroy executor shutdown ......... service/GameService.java

R6  JDBC (raw, full CRUD on its own table)
      - game_history table, create/read/update/
        delete with try-with-resources ........ dao/GameHistoryDao.java

R7  JPA / ORM (2+ entities, repositories)
      - Category (1) --< Word (many) .......... model/Category.java,
                                                 model/Word.java
      - Spring Data repositories .............. repository/*.java
      - seeding + custom word create .......... config/DataInitializer.java,
                                                 service/WordService.java

R8  RESTful web services (2+ endpoints, JSON)
      - controllers ........................... web/GameController.java,
                                                 web/CategoryController.java,
                                                 web/StatsController.java

R9  Design pattern(s)
      - Strategy (primary): TurnOrderStrategy + two implementations,
        selected at runtime .................... strategy/*.java,
                                                 service/GameService.java
      - Builder (secondary): GameSettings ..... model/GameSettings.java

R10 Exception handling
      - Unchecked hierarchy (GameException and
        subclasses) ........................... exception/GameException.java + ...
      - Checked DataAccessException at the JDBC
        layer, translated to unchecked above ... exception/DataAccessException.java,
                                                 dao/GameHistoryDao.java
      - Central @RestControllerAdvice, no empty
        catch blocks .......................... web/GlobalExceptionHandler.java


DESIGN NOTE: THE BIASED FIRST SPEAKER
-------------------------------------
Crew members get weight 1.0; each imposter gets weight w (default 0.25,
range 0..1). The first speaker is chosen by weighted (roulette) selection,
then the remaining players are shuffled uniformly. With N players and k
imposters:

    P(an imposter speaks first) = (k*w) / ((N-k) + k*w)

For N=5, k=1, w=0.25 this is about 0.059 instead of 0.20 - roughly 3.4x
less likely than a fair shuffle. A weight of exactly 1.0 disables the
bias and the uniform strategy is used instead. This was checked with a
500,000-trial Monte Carlo simulation during development.
================================================================
