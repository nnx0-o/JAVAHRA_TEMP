# Duel – jednoduchá hra pro dva

Lokální tahová hra 1v1 pro dva hráče u jedné klávesnice. Cílem je snížit zdraví soupeře na nulu. Síťový multiplayer zde není.

## Spuštění a ovládání

Spusťte `Main` v editoru, nebo použijte JDK 17 či novější:

```bash
javac -encoding UTF-8 -d out src/*.java
java -cp out Main
```

Zadejte jména hráčů; prázdný vstup použije Pepu a Mahulenu. Každý začíná se 4 životy, 10 zlatými a inventářem na 5 předmětů.

- Celá hra běží uvnitř `MiniTui`. Šipkami vybíráte prvky, Enter potvrdí tlačítko nebo zahájí/ukončí úpravu jména.
- Po zadání jmen nakupuje první hráč, poté druhý. Tlačítko „Připraven“ ukončí nákup hráče.
- Lektvar stojí 3 zlata, meč 5 a štít 4. Neúspěšný nákup nemění inventář ani zlato.
- Útok způsobí náhodně 1 nebo 2 poškození. Meč přidá 1, soupeřův štít odečte 1; stejné vybavení se nesčítá.
- Obrana sníží poškození o další 1, do začátku dalšího vlastního tahu. Poškození nikdy není záporné.
- Lektvar z inventáře obnoví až 2 životy, nejvýše do maxima 4, a spotřebuje se.
- Hru ukončíte tlačítkem „Konec“ nebo Ctrl+C.

`MiniTui` automaticky sleduje velikost terminálu na Linuxu, macOS a BSD i velikost a posun viditelné oblasti konzole ve Windows. Při zmenšení ořízne obsah, přesune stavový řádek ke spodnímu okraji a po zvětšení obsah znovu vykreslí. Souřadnice prvků, rozepsaný vstup a výběr zůstávají zachovány; použití API se nemění. Ve Windows používá systémový Windows PowerShell, na unixových systémech `stty` a `/dev/tty`.

Po platné akci se hráči vystřídají. Nedostupné léčení tah nespotřebuje. Hráč s nulovým zdravím prohrává; vítěz získá 1 zlato. Opakovaná obrana může zápas prodlužovat – limit tahů zatím není.

## Rozdělení odpovědností

- `Main` propojuje objekty a řídí celou hru: zadání jmen, nakupování, tahy, boj, obranu, použití lektvaru i vítězství. Zajišťuje také tlačítka a aktualizaci obrazovky.
- `Player` uchovává jméno, zdraví, zlato a inventář. Poskytuje malé metody pro změny vlastního stavu; neřídí souboj ani nabídku obchodu.
- `Shop` obsahuje katalog předmětů a cen. `Main` z katalogu vytváří nákupní tlačítka. Nabídka je neomezená; nákup předmět z katalogu neodebírá.
- `Item` je neměnný předmět se jménem a typem; `ItemType` určuje druh předmětu. Aktivní hra používá lektvar, meč a štít. Brnění a luk zatím nemají herní efekt.
- `ItemCollection` spravuje omezené pole předmětů. Nezná ceny ani zlato.
- `MiniTui` zajišťuje terminálové rozhraní, vstup z klávesnice a obnovování obrazovky.

`player.addToInventory(item)` přidá předmět bez platby, například jako kořist nebo odměnu. `player.buy(item, price)` nejprve ověří cenu, zůstatek a možnost přidání; při úspěchu přidá předmět a odečte cenu. Vrací `true` při úspěchu, jinak `false` bez změny stavu. Samotné odebrání předmětu z inventáře také nemění zlato.

Původní nákupní metody a kostkový souboj byly z `Player` odstraněny. Demo `MyApp` a jeho samostatná ekonomika byly odstraněny. `Hra.java` zůstává starší ukázkou výpisu hráčů; aktivní hra ji nepoužívá. Původní podklady jsou v `origin/`.

## Ověření nákupů

Jednoduchý test bez dalších knihoven ověřuje přidávání bez platby, úspěšný nákup a odmítnutí nákupu bez vedlejších změn:

```bash
javac -encoding UTF-8 -d out src/*.java tests/*.java
java -cp out PlayerPurchaseTest
```

Regresní test terminálu používá stejný překlad a spouští se příkazem `java -cp out MiniTuiTerminalTest`. Ověřuje ořezání malých oken, změny velikosti a posun viditelné oblasti i šipky na obou platformách. Na Linuxu navíc používá `script` pro skutečný pseudoterminál a kontroluje změny velikosti bez stisku klávesy, ukončení, opětovné spuštění a obnovení nastavení terminálu.

Podrobné porovnání s původní verzí a přehled dalších kroků najdete v [příručce změn](PRIRUCKA_ZMEN.txt).
