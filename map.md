# Mapa kódu hry Duel 1V1

Tahle mapa vysvětluje, **kde se co děje, kdo koho volá a proč je kód takto rozdělený**. Názvy metod odpovídají zdrojovým souborům; můžeš je hledat přes Ctrl+F.

Pro první průchod otevři [Main.java](src/Main.java). Sleduj `main` → konstruktor `Main` → `buildMenus` → akci některého tlačítka. Jakmile narazíš na `game.act`, pokračuj do [Game.java](src/Game.java). K jednotlivým výpočtům se vrať až potom.

## 1. Hlavní souvislost

Většina hry funguje v tomto pořadí:

```text
klávesa
  → MiniTui vybere nebo aktivuje tlačítko
  → uložená akce tlačítka v Main
  → metoda Game změní herní stav
  → Main.refresh aktualizuje texty a viditelnost
  → MiniTui vykreslí výsledek
```

`Main` řeší obrazovky a ovládání. `Game` rozhoduje, co hra dovolí a v jakém pořadí se věci stanou. `Player`, `Inventory` a `Damage` uchovávají konkrétní hodnoty a provádějí jejich změny.

Jde o tahovou hru: samotné překreslení obrazovky neposouvá tah, neubírá životy ani neprovádí plán.

V názvech se rozlišuje **zápas** (hra do tří výher), **souboj** (`battle`, jeden boj o výhru), **tah** (plán jednoho hráče) a běžné **kolo** (tah obou hráčů).

### Které objekty na sebe odkazují

```text
Main
 ├─ tui → MiniTui: prvky, klávesy, kreslení
 ├─ text → GameText: popisy hodnot
 └─ game → Game: jeden celý zápas
     ├─ players[0] a players[1] → dva objekty Player
     │   ├─ inventory → Inventory: vlastněné a vybavené předměty
     │   ├─ actions → seznam Item: aktuální plán
     │   └─ effects → seznam Damage: trvající účinky
     ├─ shop → Shop: společná zásoba předmětů
     └─ log → seznam textů: historie událostí
```

### Který soubor otevřít

| Soubor | Co řeší a proč existuje |
| --- | --- |
| [Main.java](src/Main.java) | Vytváří menu, ukládá jejich ovládací prvky, propojuje je s herními metodami a zobrazuje aktuální stav. |
| [Game.java](src/Game.java) | Řídí celý zápas: fáze, pořadí hráčů, obchodování, plán, provedení tahu a vítězství. |
| [Player.java](src/Player.java) | Stav jednoho hráče: životy, zlato, výdrž, štít, plán, inventář a účinky. Obsahuje například výpočet přijatého poškození. |
| [Inventory.java](src/Inventory.java) | Spravuje vlastněné kusy a odkazy na právě vybavenou zbroj a talisman. |
| [Item.java](src/Item.java) | Neměnné vlastnosti předmětu: jméno, typ, cena, síla, výdrž, délka účinku atd. |
| [ItemCatalog.java](src/ItemCatalog.java) | Konkrétní předměty a jejich hodnoty. Zde je například definice dýky a trvalé pěsti. |
| [ItemType.java](src/ItemType.java) | Kategorie předmětu: útok, léčení, štít, výdrž, zbroj, talisman. |
| [Rarity.java](src/Rarity.java) | Rarity a jejich váhy při losování obchodu. Váha není procento. |
| [Shop.java](src/Shop.java) | Vylosuje nabídku, kontroluje zásobu a provádí nákup nebo prodej. |
| [Damage.java](src/Damage.java) | Jeden trvající účinek: typ, zbývající síla a zbývající tahy. Přes název zahrnuje i léčení. |
| [GameText.java](src/GameText.java) | Z hodnot předmětu nebo účinku skládá český popis. Nerozhoduje o výsledku útoku. |
| [GameRules.java](src/GameRules.java) | Společné číselné hodnoty, například zdraví, základní výdrž a odměny. |
| [MiniTui.java](src/MiniTui.java) | Knihovna terminálu: obecné prvky, pohyb výběru, čtení kláves, vrstvy, rozměry a vykreslování. Nezná pravidla Duelu. |

## 2. Jak číst metody, lambdy a ostatní zkratky

### `private void buildMenus()` je běžná metoda

```java
private void buildMenus() {
    // telo metody
}
```

`private` znamená, že ji může přímo volat kód uvnitř třídy `Main`. `void` znamená, že nevrací hodnotu. Prázdné závorky znamenají, že nemá parametry. Tělo se provede, když někdo zavolá `buildMenus();`.

### `() -> ...` uloží práci na později

Skutečný příklad z vytvoření navigace:

```java
button(NAMES, 22, 36, "L  Historie", () -> open(HISTORY));
```

Při vytváření menu vznikne tlačítko. `open(HISTORY)` se v tu chvíli **nevolá**. Lambda `() -> open(HISTORY)` je akce, kterou si tlačítko uloží do svého pole `action`.

Parametr má typ `Runnable`: objekt s metodou `run()`, která nemá parametry ani návratovou hodnotu. Až uživatel potvrdí tlačítko, `MiniTui.handleKey` zavolá jeho `action.run()`.

Když je potřeba více příkazů, patří do složených závorek:

```java
() -> {
    message = game.buy(detailItem);
    refresh();
}
```

Po stisku se provede pokus o nákup, jeho zpráva se uloží do `message` a obrazovka se aktualizuje. Stejnou cestou se zobrazí i odmítnutí nákupu.

### `this::refresh` také předává práci

`this::refresh` zde znamená totéž jako `() -> refresh()`. Odkazuje na existující metodu tohoto objektu. Samotné `refresh()` ji naopak zavolá okamžitě.

`this` je právě vytvořený objekt `Main`. Proto například `this::startGame` nepotřebuje hledat jinou instanci aplikace.

### `case SHOP -> updateShop()` je větev přepínače

Šipka ve `switch` má jiný význam než šipka lambdy. Když běží `refresh()` a `layer` je `SHOP`, provede se tato větev hned. Žádná akce tlačítka se tu neukládá.

### Proč je ve smyčce `final int index = row`

Při vytváření tlačítek potřebuje každé tlačítko vlastní číslo řádku. Proměnná `row` se v cyklu mění, a proto ji nelze přímo zachytit v lambdě. `index` je neměnná kopie pro konkrétní průchod.

V boji si lambda pamatuje číslo řádku. Skutečný předmět přečte až při stisku z `combatEquipmentItems[index]`. Stejné tlačítko tak může po změně inventáře zastupovat jiný předmět.

### Další zápisy, které se opakují

| Zápis | Jak ho číst |
| --- | --- |
| `condition ? a : b` | Pokud podmínka platí, použij `a`, jinak `b`. |
| `List<Item>` | Seznam, jehož položky jsou předměty. |
| `new ArrayList<>()` | Vytvoření seznamu, do kterého lze přidávat a ze kterého lze odebírat. |
| `List.of(...)` | Vytvoření seznamu z vyjmenovaných položek. Tento seznam nelze rozšiřovat. |
| `List.copyOf(...)` | Kopie obsahu seznamu, kterou příjemce nemůže upravovat. Obsažené objekty se nekopírují. |
| `final` | Proměnná se později nepřepíše. U seznamu to samo o sobě nezakazuje měnit jeho obsah. |
| `static` | Člen patří třídě; nepotřebuješ pro něj vytvářet instanci. Například `GameRules.MAX_HEALTH`. |
| `null` | Chybějící odkaz: třeba dosud nevytvořená hra, prázdný slot zbroje nebo zatím nepřiřazená akce tlačítka. |

## 3. Od spuštění k hotové obrazovce

V [Main.java](src/Main.java) proběhne:

1. `main` vytvoří objekt příkazem `new Main()`.
2. Konstruktor zavolá `buildMenus()`. Vzniknou prvky všech obrazovek a uloží se jejich akce.
3. `registerKeys()` přiřadí akce klávesovým zkratkám.
4. `refresh()` doplní právě potřebný obsah a `focusDefault()` vybere počáteční prvek.
5. `onTick(this::checkTerminalSize)` zaregistruje kontrolu rozměrů pro běžící smyčku.
6. Teprve potom `tui.start()` nastaví terminál a spustí čtení kláves a kreslení.

`new Main().tui.start()` je jen zkrácený zápis „vytvoř aplikaci, vezmi její `tui`, zavolej na něm `start`“.

### Co přesně dělá `buildMenus`

Metoda má oddělené bloky pro jména, společnou navigaci, herní panely, výsledek a malý terminál. Větší části předává dalším metodám:

| Metoda v `Main` | Co vytvoří |
| --- | --- |
| `buildMatchPanel` | Skóre, Info a pravé panely obou hráčů. |
| `buildShop` | Obchod, inventář, předem vytvořené řádky a pokračování. |
| `buildCombat` | Výbavu vlevo, plán vpravo, stránkování a ukončení tahu. |
| `buildDocuments` | Společné řádky dlouhého textu, stránkování, akce detailu a text návodu. |

Prázdné texty `""` jsou místa pro budoucí údaje. Vítěz například při startu není znám. Tlačítka s akcí `null` dostanou konkrétní akci při aktualizaci svých řádků.

Pomocné metody `label`, `box`, `button` vytvoří prvek a přes `tui.add` ho zaregistrují v knihovně. `primary` vytvoří běžné tlačítko a nastaví mu výraznější vzhled.

**Pozor na pořadí souřadnic:** pomocné metody v `Main` berou nejprve vrstvu: `button(z, x, y, ...)`. Konstruktor knihovny má pořadí `new MiniTui.Button(x, y, z, ...)`. Souřadnice začínají nulou; `x` je sloupec a `y` řádek.

## 4. Vrstvy, návrat a aktualizace

### Obrazovka a herní fáze jsou dvě různé hodnoty

`Main.layer` říká, **na jaké menu se díváš**. `Game.phase` říká, **v jaké části zápasu se nacházíš**. Otevřením návodu během boje se změní menu, ale hra zůstane ve fázi `COMBAT`.

| Vrstva v `Main` | Číslo | Obsah |
| --- | --- | --- |
| `NAMES` | 0 | Jména hráčů. |
| `SHOP` | 1 | Obchod a inventář. |
| `COMBAT` | 2 | Výbava a plán tahu. |
| `DETAIL` | 3 | Detail vybraného předmětu. |
| `HELP` | 4 | Návod. |
| `HISTORY` | 5 | Historie událostí. |
| `PLAYER` | 6 | Podrobnosti vybraného hráče. |
| `RESULT` | 7 | Výsledek souboje nebo zápasu. |
| `SIZE_WARNING` | 8 | Upozornění na rozměry terminálu. |

`isAvailable` kontroluje, zda lze menu otevřít. Historie například potřebuje existující hru. `showGame` překládá fázi `Game` na hlavní obrazovku: obchod, boj nebo výsledek.

Fáze samotného zápasu postupují takto:

```text
ANNOUNCEMENT → SHOP → COMBAT → RESULT → ANNOUNCEMENT → další obchod
                         └─→ FINISHED, pokud vítěz dosáhl tří výher
```

`ANNOUNCEMENT` znamená stav připravený pro `openShop`; nemá vlastní obrazovku. Přípravné tahy jsou součást `COMBAT` a poznají se přes `isPreparation()`, tedy podle hodnoty `turn`.

### Jak funguje Esc

`previousLayers` je zásobník čísel menu. `push` přidá položku navrch, `pop` ji vezme a odstraní.

```text
otevřeš: COMBAT → HELP → HISTORY
Esc:     HISTORY → HELP → COMBAT
```

`open(next)` uloží starou vrstvu a otevře novou. `back()` vytáhne poslední uloženou vrstvu. `showRoot(next)` navíc zásobník vyčistí; používá se při přechodu mezi hráči a fázemi.

Esc vrací **menu**, nikoli dřívější stav hry. Nevrátí nákup, prodej ani odstraněnou akci. Tyto údaje zůstávají v tomtéž objektu `Game`.

### Prvky se vytvářejí jednou

V polích jako `shopContinueButton` je uložen odkaz na existující tlačítko. `setText`, `visible` nebo změna `action` mění tento objekt; nevytvářejí nové tlačítko.

Předpony říkají, kam prvek patří: `names`, `shop`, `combat`, `detail`, `result`. Skupiny `navigationElements`, `matchElements` a `documentElements` obsahují odkazy na sdílené prvky. `moveElements` jim nastaví `z` aktivního menu a viditelnost. Proto nepotřebujeme několik kopií stejného panelu hráče.

`refresh()` nejprve upraví společné prvky, potom údaje zápasu a nakonec přes `switch` doplní otevřenou obrazovku. Název `updateShop` tedy znamená „obnov texty obchodu“, nikoli „vylosuj novou nabídku“.

Textový panel sdílejí detail, návod, historie a podrobnosti hráče. `documentPages[layer]` pamatuje stránku pro každé z těchto menu. `wrapText` připraví řádky, `updateDocument` ukáže jejich výřez. Návod se do řádků převede jednou při startu.

Při malém okně se `MiniTui` přepne na varovnou vrstvu. `Main.layer` si ponechá skutečné menu, takže po zvětšení okna lze pokračovat na stejném místě. Pevný herní prostor je 140 × 38 znaků.

## 5. Nákup: celá cesta mezi soubory

```text
Game.openShop → Shop.refresh: vznikne nabídka
Main.updateShopRows: existující tlačítka dostanou text a odkaz na předmět
Enter na řádku → Main.inspect → Main.open(DETAIL)
Enter na Koupit → Game.buy → Shop.purchase → Inventory.add(item.copy())
Main.refresh: zobrazí zprávu a aktuální stav
```

[Shop.java](src/Shop.java) drží `stock`: mapu **předmět → počet kusů**. `LinkedHashMap` zachová pořadí nabídky i při snižování zásob.

`Shop.refresh` vybírá druhy podle vah z [Rarity.java](src/Rarity.java). Levné zbraně mají váhu ještě třikrát větší. Představ si losy: součet vah je počet losů a náhodné číslo určí vítězný interval. Vybraný druh se odebere z kandidátů, aby se neopakoval; počet jeho kusů se pak vylosuje zvlášť.

`purchase` ověří zásobu, zlato a místo. Nejprve zkusí vložit kopii do inventáře, až potom ubere zlato a sklad. Když není místo, hráč nepřijde o peníze.

### Proč vzniká `item.copy()`

Katalogový předmět popisuje nabídku. Každý koupený kus má vlastní objekt. Dvě dýky se stejným jménem tak mohou mít samostatné ceny opakování v plánu.

[Item.java](src/Item.java) nepřepisuje `equals`, takže `contains` zde porovnává identitu objektů. Stejně ji porovnává `==` při počítání opakovaných použití. Porovnání jmen by různé kusy chybně spojilo.

Pěst je neměnný `ItemCatalog.FIST`. Každý inventář ho obsahuje trvale; `Inventory.remove` ho odmítne odebrat. Je mimo deset míst pro nakupované kusy a není mezi losovanými nabídkami.

### Kdy obchod skončí

`Game.traded` zaznamená úspěšný nákup nebo prodej během návštěvy. `continueShop` při potvrzení návštěvy zvýší `unchangedVisits`, nebo ho po obchodování vynuluje. Dvě prázdné návštěvy za sebou spustí boj. Pouhé otevření menu, odmítnutý nákup nebo vybavení zbroje tento příznak nenastaví.

`Inventory` drží zbroj a talisman jako odkazy na vlastněné kusy. Vybavení je nepřesouvá mimo inventář. Prodej vybaveného kusu příslušný slot vyprázdní. Prodejní cena je polovina původní ceny zaokrouhlená dolů; prodaný kus se do zásob obchodu nevrací.

### Ručně vytvořené sloupce

`Main.formatShopRow` používá `String.format("%-24s | %-32s | %7s | %s", ...)`. První dva sloupce doplní mezery vpravo, cena se zarovná doprava. `fit` dlouhý text předem zkrátí. Jde stále o text jednoho tlačítka, ne o samostatný tabulkový systém.

## 6. Plán: přidání, cena a odebrání

```text
Enter na výbavě
 → Game.act(item): ověření pokusu a pracovní kopie plánu
 → Game.replacePlan: stará a nová celková cena
 → Player.replacePlan: kontrola rozpočtu a uložení
 → Main.refresh: nový plán a zbývající výdrž
```

`Player.actions` je `List<Item>` v pořadí provedení. Pěst je v tomto seznamu stejný druh položky jako jiná zbraň. `act` zatím neubírá životy a nespotřebovává lektvary.

`Game.act` odmítne například útok při přípravě, cizí předmět, pasivní vybavení, druhé zařazení stejného lektvaru nebo příliš drahý plán. Kontrola je v herní logice, takže ji nemusí kopírovat každé tlačítko.

### Cena opakování

`Game.actionCost` počítá:

```text
základní cena + počet dřívějších použití stejného kusu × příplatek
```

Příplatek určuje `Item.getRepeatCostIncrease`: polovina základní ceny zaokrouhlená na nejbližší celé číslo, nejméně 1.

`getActionCost` vrací cenu příštího přidání. `getPlannedActionCost(index)` vrací cenu již uložené akce; počítá jen položky před ní, získané přes `subList(0, index)`. Horní hranice `subList` není součástí výřezu.

### Proč se při odebrání přepočítává celý plán

Při 12 výdrže stojí tři pěsti `2 + 3 + 4 = 9`. Zbývá 3. Po odstranění první pěsti stojí zbývající dvě znovu `2 + 3 = 5`. Zbývat tedy musí 7, nikoli 5.

`Player.replacePlan` proto počítá:

```text
rozpočet = zbývající výdrž + cena starého plánu
nová zbývající výdrž = rozpočet − cena nového plánu
```

Pracovní kopie se uloží teprve po ověření rozpočtu. Neúspěšný pokus ponechá starý plán i výdrž.

Enter na řádku plánu zavolá `removePlannedAction(index)`, která vytvoří plán bez této položky a použije stejný přepočet. `PLAN_ROWS = 16` omezuje pouze počet zobrazených řádků; delší plán má další stránky.

## 7. Provedení tahu a výsledné poškození

V [Game.java](src/Game.java) sleduj `endTurn`, `resolveAction`, `beginTurn` a `finishIfDefeated`. Pořadí je součást pravidel:

1. `endTurn` vezme aktuální plán.
2. Pro každý předmět zavolá `resolveAction`.
3. Po každé akci zkontroluje smrt. Při smrti se zbytek plánu neprovede.
4. Pokud souboj pokračuje, zvýší `turn` a prohodí hráče přes `current = 1 - current`.
5. `Game.beginTurn` zavolá `Player.beginTurn`: obnoví výdrž včetně dříve připraveného bonusu a vyčistí nový plán.
6. Vyhodnotí vybavený talisman.
7. Vyhodnotí časované účinky v pořadí vzniku; po každém zkontroluje smrt.
8. Pokud souboj pokračuje, smaže starý štít.

Starý štít tak chrání i před úvodními dávkami jedu. Kdyby se smazal už v `Player.beginTurn`, změnilo by to výsledek boje.

### Co provede `resolveAction`

| Typ předmětu | Změna stavu |
| --- | --- |
| `DAMAGE`, fyzické | Okamžitý zásah soupeře přes `receiveDamage`, včetně bonusu talismanu. |
| `DAMAGE`, jed | Přidání samostatného časovaného účinku soupeři. |
| `HEAL`, délka 1 | Okamžité léčení hráče, nejvýše do maxima. |
| `HEAL`, delší | Přidání časovaného léčení hráči. |
| `DEFEND` | Přidání síly do společného štítu. |
| `STAMINA` | Přidání bonusu pro příští vlastní tah. |

Spotřební předmět se odebere až po provedení své akce. Zbroj a talisman se do plánu nezařazují; jejich účinky se čtou z vybavení.

### Dva různé `beginTurn`

`Player.beginTurn` mění zdroje a plán jednoho hráče. `Game.beginTurn` řídí celý začátek tahu, včetně talismanu, účinků a kontroly smrti. Název je stejný, ale odpovědnost jiná.

`turn` počítá dokončené tahy obou hráčů. První dva jsou přípravné: jeden pro každého. Pak se běžné kolo skládá ze dvou tahů. `getCombatRound` převádí toto počítadlo na číslo kola.

### Obrana v `Player.receiveDamage`

Odpovídající zbroj nejprve procentuálně sníží zásah a výsledek zaokrouhlí. Potom štít pohltí dostupnou část. Až zbytek sníží životy. Metoda vrátí skutečnou ztrátu životů pro zprávu do historie.

### Konec souboje a zápasu

`finishIfDefeated` určí vítěze, přidá výhru a odměny a nastaví `RESULT`, nebo po dosažení tří výher `FINISHED`. `Main.showGame` podle fáze otevře výsledek.

Po výsledku `nextBattle` obnoví bojové stavy a otočí začínajícího hráče obchodu. Boj začíná druhý hráč. Zlato a inventář zůstávají. Nový celý zápas vznikne až dalším `new Game(...)` po potvrzení jmen.

## 8. Hodnoty předmětu a časovaný účinek nejsou totéž

V [Item.java](src/Item.java) je `power` u časovaného předmětu síla **jedné dávky** a `duration` počet tahů. `getTotalPower` je vynásobí.

V [Damage.java](src/Damage.java) je naopak `amount` **celková zbývající síla** a `timer` počet zbývajících dávek. `takeTick` dávku vypočítá jako zaokrouhlené `amount / timer`, odečte ji a sníží časovač.

Například po očistě může zbývat 16 jedu na 3 tahy. Dávky budou `5 → 6 → 5`, dohromady 16. Očista ubírá celkový zbytek a nemění časovač. `Player.cleanse` má společný rozpočet a prochází jedy od nejstaršího.

`getEffects()` vrací kopii seznamu, ale objekty `Damage` uvnitř zůstávají stejné. Proto `takeTick()` skutečně změní účinek hráče a současně můžeme hotový účinek bezpečně odebrat z původního seznamu.

[GameText.java](src/GameText.java) pouze přečte tyto hodnoty: `summary` vytvoří krátký popis do řádku, `description` detail předmětu a `effect` zbývající sílu a tahy. Úprava jejich textu sama nezmění pravidla.

## 9. Co se odehrává uvnitř MiniTui

Pro spojení s aplikací jsou podstatné tyto části [MiniTui.java](src/MiniTui.java):

| Část | Úloha |
| --- | --- |
| `Element`, `Box`, `Label`, `Input`, `Button` | Společné vlastnosti a konkrétní druhy prvků. `Title` a `TableRow` jsou další obecné prvky knihovny. |
| `add` | Uloží prvek do seznamu a vrátí odkaz na stejný objekt. Proto lze výsledek uložit do pole v `Main`. |
| `onKey`, `onTick` | Registrují práci pro pozdější volání. |
| `start` | Nastaví terminál a spustí hlavní smyčku. |
| `readUnixKeys`, `UnixKeyDecoder`, `readWindowsKeys` | Převedou vstup terminálu na pojmenované klávesy. |
| `handleKey` | Zpracuje zkratku, psaní do vstupu, pohyb výběru nebo Enter na tlačítku. |
| `setLayer`, `focus`, `moveFocus` | Určují zobrazovanou vrstvu a vybraný ovládací prvek. |
| `renderScreen`, `renderBoundary`, `appendClipped` | Sestaví obraz a oříznou ho podle prostoru. |
| `updateUnixTermSize`, `updateTerminalGeometry` | Zjistí a uloží rozměry; odpovědi terminálu se nezapisují do jména. |
| `restoreTerminal` | Při ukončení vrátí běžné nastavení konzole. |

Vstupní vlákno čte klávesy a ukládá je do `keyQueue`. Hlavní smyčka frontu vybírá, zavolá obsluhu, spustí tick a vykreslí obrazovku. Akce měnící hru tak běží ve stejném vlákně jako obsluha tlačítek.

Tick běží přibližně každých 50 ms. V této aplikaci přes něj kontrolujeme rozměry; neprovádíme přes něj herní tahy. Vykreslování si pamatuje minulý snímek, takže stejný obraz nemusí tisknout znovu. Vrstva `z` zde určuje celé menu, nikoli průhlednou vrstvu nad jinými menu.

## 10. Kam sáhnout, když chceš něco změnit

| Chci změnit… | Kde začít |
| --- | --- |
| Zdraví, základní výdrž, odměny, počet výher | [GameRules.java](src/GameRules.java). |
| Sílu, cenu nebo délku konkrétního předmětu | Jeho řádek v [ItemCatalog.java](src/ItemCatalog.java). Pomocné metody `weapon`, `heal` atd. doplňují společné vlastnosti kategorie. |
| Příplatek za opakované použití | `Item.getRepeatCostIncrease` v [Item.java](src/Item.java). Počítání použití je v `Game.actionCost`. |
| Kdy se provede útok, léčení nebo bonus | `resolveAction` a `beginTurn` v [Game.java](src/Game.java). |
| Pořadí zbroje, štítu a životů | `receiveDamage` v [Player.java](src/Player.java). |
| Očistu a výpočet zbývajícího účinku | `cleanse` v [Player.java](src/Player.java) a `takeTick` / `reduceRemaining` v [Damage.java](src/Damage.java). |
| Nový druh pasivního účinku | `Item.Passive`, jeho definici v katalogu, vyhodnocení v `Game.beginTurn` nebo `attackBonus` a popis v `GameText`. |
| Šance nabídky a množství kusů | `Shop.refresh`, `selectionWeight` a hodnoty rarity nebo předmětů. |
| Text, pozici nebo akci tlačítka | Odpovídající `build...` v [Main.java](src/Main.java); proměnlivý obsah v odpovídajícím `update...`. |
| Sloupce obchodu | `Main.formatShopRow`. |
| Texty detailů nebo souhrnů | [GameText.java](src/GameText.java). |
| Dostupnost menu nebo návrat | `Main.isAvailable`, `open`, `back`, `showRoot`. |
| Počet zobrazených řádků a polohy prvků | Konstanty a metody vytvářející obrazovky v [Main.java](src/Main.java). Menší počet řádků znamená více stránek. |
| Obecné čtení kláves a kreslení terminálu | [MiniTui.java](src/MiniTui.java). |

Některé změny mají dvě části. Zvýšit sílu existující dýky znamená změnit číslo v katalogu. Zavést úplně nový druh akce znamená doplnit jeho kategorii, provedení a popis. Změna počtu míst v inventáři vyžaduje také zkontrolovat, zda se řádky vejdou do pevného rozhraní.

## 11. Jak to bylo před posledním zjednodušením

Tato tabulka porovnává konkrétní změny struktury a rozhraní z posledních úprav:

| Dříve | Aktuálně | Důvod změny |
| --- | --- | --- |
| Úvodní obrazovka jen s tlačítkem Začít. | Program otevře jména hráčů. | Jedna obrazovka a jeden přechod odpadly. |
| Kopie panelů hráčů pro několik menu; aktualizace i skrytých obrazovek. | Jeden společný panel a aktualizace aktivní obrazovky. | Stejné údaje se nemusí udržovat na několika místech. |
| Oddělené sady textových řádků pro detail, návod, historii a hráče. | Jeden textový panel; každé menu si pamatuje svou stránku. | Stejné stránkování slouží více obsahům. |
| Pěst měla zvláštní příznak `basicAttack` v objektu akce. | Plán je seznam předmětů a pěst je trvalý `Item`. | Stejný výpočet ceny a provedení jako u dalších zbraní. |
| Vybrat položku plánu a pak stisknout Odebrat. | Enter na položce ji rovnou odstraní. | Odpadl zvláštní stav vybrané akce. |
| UI předem kontrolovalo část pravidel a vypínalo tlačítka. | Platnost pokusu kontroluje hra a vrací zprávu. | Pravidlo je na jednom místě. |
| Dlouhé zvláštní popisy a rozepsané budoucí dávky účinků. | Jednotný přehled vlastností, společná pravidla v návodu a zbývající síla účinku. | Méně opakovaného textu a formátovací logiky. |
| Sloupce obchodu se posouvaly podle délky textu. | Pevné šířky doplněné mezerami. | Cena a zásoba začínají na stejných místech. |

Prvky rozhraní se nadále vytvářejí **jednou předem**. Po akci se mění jejich obsah a viditelnost. Pravidla a pořadí dějů hledej v herních třídách; seznam obrazovek, jejich prvky a propojení s akcemi jsou v `Main`.
