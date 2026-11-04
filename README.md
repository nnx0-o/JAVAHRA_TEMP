# Duel – jednoduchá hra pro dva

Lokální tahová hra 1v1 pro dva hráče u jedné klávesnice. Cílem je snížit zdraví soupeře na nulu. Síťový multiplayer zde není.

## Spuštění a ovládání

Spusťte [Main](src/Main.java) v editoru, nebo použijte JDK 17 či novější:

```bash
javac -encoding UTF-8 -d out src/*.java
java -cp out Main
```

Zadejte jména hráčů; prázdný vstup použije Pepu a Mahulenu. Každý začíná se 4 životy a 10 zlatými.

- Celá hra běží uvnitř `MiniTui`. Šipkami vybíráte prvky, Enter potvrdí tlačítko nebo zahájí/ukončí úpravu jména.
- Po zadání jmen nakupuje první hráč, poté druhý. Tlačítko „Připraven“ ukončí nákup hráče.
- Lektvar stojí 3 zlata, meč 5 a štít 4. Nákup používá skutečný inventář a peníze hráče.
- Útok způsobí náhodně 1 nebo 2 poškození. Meč přidá 1, soupeřův štít odečte 1; stejné vybavení se nesčítá.
- Obrana sníží poškození o další 1, do začátku dalšího vlastního tahu. Poškození nikdy není záporné.
- Lektvar z inventáře obnoví až 2 životy, nejvýše do maxima 4, a spotřebuje se.
- Hru ukončíte tlačítkem „Konec“ nebo Ctrl+C.

Po platné akci se hráči vystřídají. Neplatný vstup ani nedostupné léčení tah nespotřebují. Hráč s nulovým zdravím prohrává; vítěz získá 1 zlato. Opakovaná obrana může zápas prodlužovat – limit tahů zatím není.

## Změny oproti prvnímu commitu

Srovnání vychází z prvního commitu `54e193b` (`init`). Následující přehled zahrnuje i změny provedené v pozdějších commitech a aktuálním pracovním adresáři.

### Původní verze

Projekt obsahoval `Main`, `Hrac`, `Predmet`, `Pole` a `DruhPredmetu` přímo v kořeni. `Main` vytvořil Pepu a Mahulenu, nakoupil předměty a automaticky provedl dvě výzvy na souboj. Hráč nemohl vybírat akce. Oba účastníci hodili kostkou; vyšší hod přidal vítězi zlato a odebral soupeři život. Remíza nic nezměnila. Nebyl zde celý zápas ani vyhodnocení smrti.

### Postupné změny

- České názvy byly převedeny na anglické: `Hrac` → `Player`, `Predmet` → `Item`, `Pole` → `ItemCollection`, `DruhPredmetu` → `ItemType`.
- Zdroje jsou v `src`; nastavení VS Code určuje výstup do `bin`.
- Hráč má gettery. Předmět má neměnný název a typ, přístupné přes gettery.
- Inventář kontroluje záporné indexy, odmítá `null` a poskytuje počet položek i informaci o zaplnění.
- Nákup při plném inventáři už nestrhává zlato.
- Přibylo terminálové rozhraní [MiniTui](src/MiniTui.java) a demonstrační aplikace [MyApp](src/MyApp.java).
- Ekonomika demonstrace byla vyčleněna do [Shop](src/Shop.java): nákupy, pasivní příjem, horník, odměny a kupon.

### Nový hratelný základ

- Samostatná třída `Duel` byla odstraněna. Dočasný stav souboje a pravidla tahů patří do `Main`.
- [Player](src/Player.java) spravuje vlastní zdraví: poškození nesníží zdraví pod nulu a léčení nepřekročí maximum.
- [Main](src/Main.java) propojuje hráče, obchod a TUI. Obsahuje zadání jmen, přípravu, pravidla boje i vyhodnocení vítěze.
- Popisky se obnovují přes `onTick`, všechny akce vyvolávají tlačítka TUI. Nejsou zde žádné vstupní prompty přes `Scanner`.
- [Game.java](src/Game.java) zůstává celý zakomentovaný jako snímek dřívějšího koordinátoru. Není to přesná kopie prvního commitu a nespouští se.
- Nový kód obsahuje české komentáře k odpovědnostem a důležitým pravidlům.

## Starší demonstrační kód

`MyApp` zůstává oddělená demonstrace původního dashboardu. Její starší metody v `Shop` mají vlastní ekonomiku; nová hra je nepoužívá. Herní nákup probíhá přes `Shop.buy(Player, ItemType)` a mění konkrétního hráče. Původní `Player.challengeToDuel()` zůstává pro komentovanou starší ukázku; nový souboj ho nepoužívá.

## Další malé kroky

1. Přidat odvetu a skóre zápasů.
2. Zobrazit podrobný seznam inventáře v TUI.
3. Doladit ceny a sílu vybavení podle odehraných zápasů.

Záměrem je nejprve přehledný duel, bez mapy, questů a složitého systému schopností.
