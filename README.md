# Duel pro dva

Lokální tahová hra pro dva hráče u jedné klávesnice. Vyhrává první hráč se třemi vítěznými souboji. Rozhraní i popisy předmětů jsou v češtině.

## Spuštění

Použij JDK 17 nebo novější a skutečný interaktivní terminál, alespoň 60 × 24 znaků. Doporučená šířka je 100 znaků, aby byly přehledně vidět statistiky obou hráčů.

```bash
javac -encoding UTF-8 -d build/check src/*.java
java -cp build/check Main
```

Šipky vybírají prvek, Enter potvrdí tlačítko nebo zahájí a ukončí úpravu jména. Ctrl+C hru ukončí. Českou diakritiku lze používat i ve jménech. Zmenšení okna pod minimální rozměry pozastaví ovládání; po zvětšení lze pokračovat. Hra se neukládá po ukončení programu.

## Průběh zápasu

1. Zadejte jména a potvrďte připravenost. Hra vylosuje prvního nakupujícího; druhý nakupující začne souboj.
2. Každý začíná s **15 zlatými**, **40 životy**, **12 výdrže** a inventářem na **10 předmětů**.
3. Obchod obsahuje **10 různých společných nabídek**, vybraných ze **76 předmětů**. Každá má vlastní rozsah počtu kusů a mírnou váhu podle vzácnosti. Menší terminál nabídku rozdělí do stránek.
4. Kliknutí na nabídku otevře popis a možnost nákupu. Inventář umožňuje přečíst popis, vybavit zbroj či talisman a prodat předmět. Prodej vrací polovinu ceny **zaokrouhlenou dolů**; předmět zmizí a nevrací se do obchodu.
5. „Pokračovat“ předá obchod druhému hráči. Teprve dvě návštěvy za sebou bez úspěšného nákupu nebo prodeje zahájí boj. Dosavadní nákupy, zlato ani sklad se při předání neresetují.
6. Oba hráči mají nejprve jeden vlastní přípravný tah bez útoků. Mohou použít štíty, léčení a lektvary výdrže.
7. Poté hráč používá akce do vyčerpání výdrže nebo podle svého rozhodnutí. Akce se provádějí ihned. Tlačítkem „Ukončit tah“ předá hru soupeři. Stavy, vybavení a časované účinky obou hráčů jsou veřejné.
8. Smrt okamžitě ukončí souboj. Vítěz dostane **6 zlata**, poražený **9**. Do dalšího souboje zůstávají peníze a předměty; zdraví, výdrž a bojové účinky se resetují. Pořadí obchodu a boje se obrátí bez ohledu na vítěze.

## Bojová pravidla

- Výdrž se obnovuje na začátku vlastního tahu. Těžká zbroj může snížit její základ. Bonus z lektvaru se přidá příští vlastní tah a může překročit maximum, například na 14/12.
- Každý kus zbraně a štítu lze použít jednou za vlastní tah. Více samostatných kusů lze použít zvlášť. Lektvary léčení a výdrže se spotřebují. Základní útok je zdarma, jednou za tah, za 3 fyzického poškození.
- Na začátku tahu působí nejprve vybavený talisman, potom časované účinky v pořadí vzniku. Léčení vytvořené před jedem tedy působí dříve. Pokud ale dřívější účinek hráče zabije, pozdější léčení už nepomůže.
- Fyzické poškození působí okamžitě. Jed působí postupně; každá aplikace má vlastní časovač. Oheň se sloučí do jediného účinku: sečte zbývající poškození a ponechá delší zbývající trvání.
- Dávka časovaného účinku je zbývající celková hodnota dělená zbývajícím počtem tahů, zaokrouhlená na nejbližší celé číslo. Poloviny se zaokrouhlují nahoru. Skutečná dávka se odečte ze zbytku, takže se zachová celkový účinek. Například 15 poškození na čtyři tahy: **4, 4, 4, 3**.
- Zbroj snižuje určitý typ poškození procentem nebo odečítá pevnou hodnotu při splnění prahu. Prahová zbroj vždy propustí alespoň polovinu zásahu zaokrouhlenou na nejbližší celé číslo; aktivní štít může pohltit i tento zbytek. Pak se uplatní aktivní štít. Štít má společnou zásobu pro všechny typy poškození, platí ihned a zanikne po úvodních účincích příštího vlastního tahu.
- Léčení s trváním jeden tah působí ihned. Delší léčení začne na začátku příštího vlastního tahu a nikdy nepřekročí maximum života.
- Lze vybavit jednu zbroj a jeden talisman, pouze v obchodě. Vybavené předměty stále zabírají místo v inventáři. Talisman může léčit, čistit jed či oheň, přidávat výdrž, posílit fyzické útoky nebo rozšířit inventář. Před sundáním či prodejem brašny se musí zbývající obsah vejít.

Očistné talismany odstraní před působením účinků nejvýše 3 zbývajícího poškození příslušného typu za vlastní tah. U jedu se tento rozpočet rozdělí od nejstaršího účinku; nejde o imunitu. Zbraně do 7 zlata mají trojnásobnou váhu při losování obchodu. Žádná nabídka ani kategorie není zaručena.

Čísla jsou výchozí pro další hraní a ladění. Nejde o finálně vyváženou ekonomiku. Obecná nastavení jsou v `src/GameRules.java`; jednotlivé ceny, síly, náklady, zásoby a popisy v `src/ItemCatalog.java`. Hra zatím nemá limit tahů ani automatické řešení patové situace.

## Struktura kódu

| Soubor | Odpovědnost |
|---|---|
| `Main.java` | Jména, obrazovky, tlačítka, stránkování a propojení s hrou |
| `Game.java` | Fáze zápasu, pořadí hráčů, akce, pasivní účinky, vítězství a odměny |
| `GameRules.java` | Výchozí hodnoty pro snadné ladění |
| `Player.java` | Stav hráče, historie akcí v tahu, výdrž, účinky a příjem poškození |
| `Inventory.java` | Vlastněné kusy, kapacita a odkazy na vybavení |
| `Damage.java` | Zbývající celkový účinek, časovač, zaokrouhlení a slučování ohně |
| `Item.java`, `ItemType.java`, `Rarity.java` | Neměnné vlastnosti předmětů a jejich druhy |
| `ItemCatalog.java` | 76 předmětů a jejich české popisy |
| `Shop.java` | Náhodná nabídka, zásoby, nákup a prodej |
| `MiniTui.java` | Terminál, vykreslování, klávesnice a změny velikosti |
| `Test.java` | Samostatná ukázka rozhraní; není hlavní hra |

Podrobný anglický průvodce změnami, rozhodnutími a důvody je v `.leave/progress.txt`. Adresář `.leave` je podle stávajícího `.gitignore` lokální a nesleduje se v Gitu.

## Ověření

Testy nevyžadují externí knihovny:

```bash
javac --release 17 -encoding UTF-8 -d build/check src/*.java tests/*.java
java -cp build/check DamageTest
java -cp build/check GameTest
java -cp build/check MainUiTest
java -cp build/check TerminalInputTest
java -cp build/check BalanceRegressionTest
```

Na Linuxu lze navíc spustit skutečnou terminálovou zkoušku přes Python 3:

```bash
python3 tests/terminal_smoke.py
```

Volitelný první argument je cesta k programu `java`. Zkouška projde zadání jména s diakritikou, obchod, přípravu, útok a ukončení. Terminálový záznam uloží do `build/terminal-smoke.txt`.

## Textová verze a opakovatelné hraní

`Playtest` nabízí stejného hráče, obchod a bojový model přes řádkové příkazy. Používá stejná pravidla jako `Main`, nikoli zjednodušenou kopii. Výchozí jména v obou verzích jsou Hráč 1 a Hráč 2.

```bash
java -cp build/check Playtest 17
```

Číslo je semínko náhody, takže stejné příkazy vytvoří stejný průběh. `shop` otevře obchod, `buy N` koupí nabídku, `equip N` vybaví vlastní předmět, `use N` ho použije a `attack` provede základní útok. `end` předá obchod nebo ukončí bojový tah. `next` pokračuje po výsledku, `log` vypíše historii a `quit` skončí. `offer N` a `item N` zobrazí popis. Po spotřebování nebo prodeji se čísla vlastních předmětů posunou podle aktuálního výpisu.

Automatický zátěžový test odehraje pevné souboje i náhodné zápasy:

```bash
java -cp build/check BalanceSimulation 1000 10000 balanced
java -cp build/check BalanceSimulation 1000 10000 defensive
java -cp build/check BalanceSimulation 1000 10000 aggressive
```

Argumenty určují počet zápasů, posun semínek a strategii. Limit 160 tahů je pouze pojistka testu, nikoli pravidlo hry. Strategie jsou jednoduché heuristiky; výsledky nejsou důkazem optimálního hraní. Výsledky první úpravy vyvážení a její omezení popisuje [zpráva z hraní](playtests/balance-report.md).
