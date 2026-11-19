# Duel 1V1

Lokální hra pro dva hráče u jedné klávesnice. Hráči nakupují vybavení, plánují akce a snaží se vyhrát tři souboje.

## Spuštění

Ve VS Code použij **Run Java** nad metodou `main`, nebo vyber konfiguraci **Duel** a stiskni F5. Hra se na Windows a Linuxu s desktopem otevře v novém terminálovém okně, které se po ukončení hry zavře. Na Linuxu se použije `x-terminal-emulator` nebo `xterm`. Pokud launcher není dostupný, hra zůstane v původním terminálu, stejně jako na macOS a bez desktopu.

V samostatném terminálu, otevřeném ve složce projektu:

```bash
src/oracleJdk-27/bin/javac --release 17 -encoding UTF-8 -d bin src/*.java
src/oracleJdk-27/bin/java -cp bin Main
```

Pokud máš JDK v systémové cestě, stačí `javac` a `java`. Adresář `src/oracleJdk-27` obsahuje přibalené JDK, nikoli zdrojový kód hry. `bin` je vygenerovaný výstup kompilace.

Pro spuštění přímo v původním terminálu použij `java -Dminitui.currentTerminal=true -cp bin Main`.

Rozhraní potřebuje **140 × 38 znaků**. Šipky vybírají, Enter potvrzuje, F1/H otevře návod, I popis výbavy, R obnoví obsah a Esc vrací předchozí menu. Ctrl+C ukončí hru.

## Který soubor má jaký důvod

| Soubor | Co má na starosti |
| --- | --- |
| [Main.java](src/Main.java) | Vstup programu, vytvoření menu, klávesy, vrstvy a aktualizace údajů na obrazovce. |
| [Game.java](src/Game.java) | Pořadí hráčů, fáze zápasu, obchodování, plánování, provedení akcí a vítězství. |
| [Player.java](src/Player.java) | Stav jednoho hráče: životy, výdrž, zlato, štít, plán a účinky. |
| [Inventory.java](src/Inventory.java) | Vlastněné kusy předmětů a odkazy na vybavenou zbroj a talisman. |
| [Item.java](src/Item.java) | Neměnné hodnoty jednoho předmětu a vytvoření kopie pro nákup. |
| [ItemCatalog.java](src/ItemCatalog.java) | Seznam předmětů a jejich hodnoty. |
| [ItemType.java](src/ItemType.java), `Item.Rarity` v [Item.java](src/Item.java) | Kategorie předmětu a váha při losování nabídky. |
| [Shop.java](src/Shop.java) | Společná zásoba, losování, nákup a prodej. |
| [Damage.java](src/Damage.java) | Zbývající síla a délka jednoho časovaného účinku. |
| [GameText.java](src/GameText.java) | České popisy odvozené přímo z hodnot předmětu nebo účinku. |
| [GameRules.java](src/GameRules.java) | Společné číselné konstanty pro ladění hry. |
| [MiniTui.java](src/MiniTui.java) | Podpůrná knihovna: prvky, klávesnice, vrstvy a vykreslování terminálu. Neobsahuje pravidla souboje. |

## Jak program projít před prezentací

### 1. Od spuštění k tlačítku

`Main.main` nejprve přes `MiniTui.openTerminal` zkusí spustit hru v novém terminálu. Proces ve vlastním terminálu pak vytvoří aplikaci a spustí `MiniTui.start`. Konstruktor aplikace jednou vytvoří všechny nabídky a zaregistruje jejich akce.

MiniTui má vstupní vlákno, které čte klávesy a ukládá je do fronty. Hlavní smyčka klávesy zpracuje, spustí odpovídající akci a vykreslí obrazovku. Herní změny proto probíhají v hlavní smyčce.

Zápis `() -> open(HELP)` ukládá práci pro pozdější stisk tlačítka. Během vytvoření tlačítka se `open` ještě nevolá.

### 2. Přepínání nabídek

Každé menu má číslo vrstvy `z`. MiniTui vykresluje jen aktuální vrstvu. Prvky ostatních menu zůstávají vytvořené.

Hra začíná zadáním jmen. `layer` uchovává aktuální menu a `previousLayers` je zásobník čísel předchozích menu. `open` nejprve ověří dostupnost nové vrstvy a potom uloží starou. `back` vezme poslední číslo ze zásobníku. Prázdný zásobník znamená, že se není kam vrátit.

Prvky mají předponu podle obrazovky: například `shopContinueButton` nebo `combatPlanButtons`. Společné prvky mají předpony `menu`, `match` a `document`. Panel zápasu a textový panel existují pouze jednou; jejich `z` se přesouvá na aktivní vrstvu.

`refresh` se volá po akci nebo při přechodu mezi menu. Aktualizuje společný panel a právě otevřenou obrazovku. Nemění životy ani neprovádí akce. Při přechodu na jiného hráče nebo do další fáze se historie návratů vyčistí. Návod se zalomí do řádků jednou při vytvoření menu.

### 3. Obchod a vlastnictví

`Game` vlastní oba hráče a jeden společný `Shop`. Nabídka je mapa předmět → počet kusů. Rarita určuje váhu losování a levné zbraně mají vyšší váhu. Vybraný druh se odebere ze seznamu kandidátů, aby se ve stejné nabídce neopakoval.

Při nákupu vznikne `item.copy()`. Nabídka a každý koupený kus tak mají vlastní identitu. Dvě stejně pojmenované zbraně jsou dva různé objekty.

`traded` říká, zda hráč během své návštěvy nakupoval nebo prodával. Dvě návštěvy za sebou bez obchodování zahájí souboj. Nákup ani prodej zásobu pro druhého hráče neobnoví.

### 4. Plánování a rezervace výdrže

`Player.actions` je seznam předmětů v pořadí provedení. Pěst je běžný předmět definovaný jako `ItemCatalog.FIST`, který má každý hráč trvale v inventáři. Nelze ji prodat ani odebrat; vedle ní zůstává deset míst pro koupené předměty. Útok pěstí používá stejnou cestu jako jiná fyzická zbraň.

Enter na výbavě přidá předmět do plánu. Enter na řádku plánu jej rovnou odebere. Tlačítka ukazují cenu akce; samotná hra ověří její platnost a případné odmítnutí vypíše do panelu Info.

`Game.act` zkontroluje přípravnou fázi, vlastnictví, spotřební kus a cenu plánu. Při úspěchu rezervuje výdrž a uloží plán. Poškození ani léčení zatím neproběhne.

Cena opakovaného použití vychází z počtu předchozích použití stejného kusu v plánu. Porovnání `==` je zde záměrné: jiná kopie stejné zbraně je jiný kus.

Příklad: při 12 výdrže stojí tři pěsti 2 + 3 + 4, zbývají tedy 3. Po odebrání první pěsti stojí zbývající dvě znovu 2 + 3, takže zbývá 7. Proto se přepočítá celý plán, nikoli jen cena odebrané akce.

Rozpočet pro změnu plánu je `zbývající výdrž + stará cena plánu`. Od něj se odečte nová cena. Když změna nevyjde, původní plán i výdrž zůstanou stejné.

### 5. Provedení tahu

`Game.endTurn` prochází plán shora dolů. Každou akci provede `resolveAction`. Po každé akci se kontroluje smrt; při smrti se zbytek plánu neprovede.

Pak se vystřídá hráč výrazem `1 - current` a zavolá se `beginTurn`. Ten obnoví výdrž včetně bonusu z předchozího tahu, vyčistí plán, vyhodnotí talisman a časované účinky. Starý štít chrání i před úvodními účinky a teprve potom se smaže.

Přípravné tahy používají stejný plán, ale útoky jsou během nich zakázané.

### 6. Účinky, obrana a konec zápasu

`Damage` drží celkovou zbývající sílu a počet zbývajících tahů. Dávka je zaokrouhlené `amount / timer`. Skutečná dávka se odečte a časovač sníží o jeden, aby se neztratil celkový součet.

Účinky zůstávají v pořadí vzniku. `getEffects` vrací kopii seznamu, takže během jejich procházení lze hotové účinky bezpečně odstranit z hráče. Léčení se omezuje maximem životů; poškození nejprve upraví odpovídající zbroj, potom ho pohltí štít a zbytek sníží životy.

Při smrti `finishIfDefeated` přidá výhru a odměny. Tři výhry znamenají konec zápasu. Jinak `nextBattle` obnoví bojové stavy a otočí pořadí začínajících hráčů; peníze a předměty zůstávají.

## Rozměry terminálu

MiniTui používá `stty` jako zálohu a zároveň se ptá samotného terminálu na rozměry. Dotazy na velikost okna mohou být vypnuté, proto navíc uloží polohu kurzoru, posune jej do pravého dolního rohu, zeptá se na jeho souřadnice a vrátí jej zpět. Platná odpověď má přednost před zastaralou velikostí pseudo-terminálu a nevloží se do rozepsaného jména. Podporované sekvence a omezení dotazů popisuje [dokumentace xterm.js](https://xtermjs.org/docs/api/vtfeatures/) a [nastavení windowOptions](https://xtermjs.org/docs/api/terminal/interfaces/iwindowoptions/).
