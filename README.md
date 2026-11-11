# Duel 1V1

Lokální hra pro dva hráče u jedné klávesnice. Nakupte vybavení, naplánujte akce a vyhrajte tři souboje. Rozhraní, předměty, popisy i návod jsou v češtině. Angličtina zatím není v nabídce.

## Spuštění

JDK 17 nebo novější, interaktivní terminál alespoň **100 × 32 znaků**. Doporučená velikost je **140 × 42**.

```bash
javac --release 17 -encoding UTF-8 -d build/check src/*.java
java -cp build/check Main
```

Volitelně lze změnit mezeru mezi hlavním obsahem a pravými panely:

```bash
java -Dduel.panelGap=4 -cp build/check Main
```

Šipky vybírají prvky i doleva a doprava, Enter potvrzuje. **F1** nebo **H** otevře návod, **L** historii, **Esc** vrací předchozí pohled, **Ctrl+C** ukončí hru. Při výběru bojové výbavy zobrazí **I** popis vybraného předmětu. Během úpravy jména se písmena zapisují do jména. Zmenšení okna pod minimum pozastaví ovládání, nikoli uložený stav zápasu.

## Orientace v rozhraní

- Bílá patří úvodním a výsledkovým obrazovkám; oranžová prvnímu hráči a mátová druhému. Záhlaví ukazuje souboj, fázi a aktuálního hráče.
- Vlevo nahoře je skóre a pořadí začátku obchodu a boje. Samostatný rámeček pod ním zobrazuje zprávy.
- Vpravo zůstávají oba hráči: životy, štít, vybavení a účinky. Zlato je vidět v obchodě a po výsledku. Zbývající výdrž je přímo v záhlaví bojové výbavy a detailu předmětu. Podrobnosti rozbalí úplný stav hráče.
- Obchod má kompaktní sloupce předmětu, účinku, ceny a množství, s jemným střídáním pozadí celých řádků. Jejich šířka se na velkém terminálu neprotahuje bez omezení. V záhlaví obchodu vidíš své zlato. Zlato je žluté, dostupné kusy zelené, vyprodané červené. Nedostupný nákup je vysvětlený a vypnutý.
- Návod a historie jsou globální ovládací prvky v patičce. Otevření ani zavření nespotřebuje tah a nemaže plán.

## Zápas a obchod

Každý začíná s **15 zlatými**, **40 životy**, **12 výdrže** a **10 místy v inventáři**. Po zadání jmen hra přejde do obchodu. První nakupující se losuje a druhý začne boj; příští souboj si pořadí obrátí.

Deset náhodných nabídek vychází ze 76 předmětů. Oba hráči nakupují stejnou zásobu, která se při předání neresetuje. Zbraně do 7 zlata mají trojnásobnou váhu výběru; žádná kategorie není zaručená. Rarita mění pravděpodobnost, množství má každý předmět vlastní rozsah.

V obchodě lze nakupovat, prodávat a měnit vybavení. Prodej vrátí polovinu ceny **zaokrouhlenou dolů** a předmět odstraní. Pokud dva hráči za sebou pokračují bez nákupu či prodeje, začne boj. Tlačítko vždy říká, zda předá obchod konkrétnímu hráči, nebo zahájí souboj.

Vítěz dostane **6 zlata**, poražený **9**. V dalším souboji zůstávají peníze a nevyužité předměty; zdraví, výdrž a bojové stavy se resetují. Tři vítězství ukončí zápas. Výsledek uvádí odměny u jmen hráčů.

## Plánování a pořadí akcí

Každý hráč dostane jeden přípravný tah bez útoků. Potom začíná **kolo 1**, složené z tahů obou hráčů. Záhlaví ukazuje i pozici v kole, například `Kolo 1 (2/2)`.

Výběr akce ji přidá do viditelného plánu a rezervuje výdrž. **Životy, štíty, léčení, spotřeba lektvarů a nové stavy se mění až při potvrzení konce tahu.** Plán se provede shora dolů a při smrti se další akce zastaví. Vybranou akci lze odebrat; rezervace výdrže se přepočítá. Pořadí odpovídá pořadí přidávání.

Pěst je trvalou součástí výbavy, nestojí zlato a nezabírá inventář, ale stojí výdrž. První použití stojí 2, další ve stejném tahu 3, 4 atd. Zbraně a štíty jsou opakovatelné a mají vlastní přírůstek ceny; cena se obnoví na začátku dalšího vlastního tahu. Lektvar lze naplánovat jednou na fyzický kus a spotřebuje se až při provedení.

Na začátku vlastního tahu se obnoví výdrž, působí talisman a potom časované účinky v pořadí vzniku. Bonus výdrže může překročit běžné maximum. Starý štít kryje i úvodní účinky a potom zanikne.

## Poškození a předměty

Fyzické poškození působí při vyhodnocení akce. Jed a oheň působí na začátku dalších tahů cíle. **Síla časovaného předmětu znamená dávku za tah**, Všude se používá zápis **`Jed: 4 × 3 (12)`**: síla × počet tahů (celkem). Léčení používá stejný zápis. Přímé poškození se zkracuje například na `3 fyzické`. Uvedené poškození je před obranou cíle.

Sloučení ohně nebo očista mohou změnit zbývající dávky. Stav pak ukazuje přesnou posloupnost a celkový zbytek, například `Oheň: 5 → 6 → 5 (16)`; neslibuje konstantní dávku, pokud by součet neseděl.

Oheň sčítá zbývající celkové poškození a ponechá delší trvání. Jedy zůstávají samostatné. Dávka je zbývající součet dělený zbývajícím počtem tahů, zaokrouhlený na nejbližší celé číslo. Při dělení se odečítá skutečná dávka, aby se zachoval součet. Životy jsou celá čísla. Výjimkou zaokrouhlování je prodejní cena, která se zaokrouhluje dolů.

Zbroj působí před štítem. Odolnost snižuje konkrétní typ poškození; prahová zbroj odečítá při splnění podmínky a propustí alespoň polovinu zásahu zaokrouhlenou nejbližším celým číslem. Aktivní štít může pohltit i zbytek. Očistné talismany odebírají nejvýše 3 zbývajícího poškození daného typu za vlastní tah, ne celý stav bez omezení.

Vybavit lze jednu zbroj a jeden talisman, pouze v obchodě. Vybavení zabírá místo v inventáři. Brašna přidá místa při vybavení; při sundání či prodeji se zbývající obsah musí vejít. Čísla pro ladění jsou v `src/GameRules.java` a `src/ItemCatalog.java`. Názvy a popisy odvozené z aktuálních hodnot spravuje `src/GameText.java`.

Hra zatím nemá limit tahů, automatickou remízu, ukládání po ukončení ani síťové hraní. Předchozí zpráva z vyvažování v `playtests/balance-report.md` zachycuje starší pravidla před zavedením plánů a placené opakovatelné pěsti.

## Ověření

```bash
javac --release 17 -encoding UTF-8 -d build/check src/*.java tests/*.java
java -cp build/check DamageTest
java -cp build/check GameTest
java -cp build/check QueuedCombatTest
java -cp build/check TimedItemTest
java -cp build/check MainUiTest
java -cp build/check MiniTuiWidgetTest
java -cp build/check TerminalInputTest
java -cp build/check BalanceRegressionTest
python3 tests/terminal_smoke.py
```

`MainUiTest` ukládá skutečně vykreslené terminálové snímky do `build/ui-snapshots`. Volitelný převod do PNG vyžaduje Python s Pillow:

```bash
python3 tests/render_terminal.py
```

Řádková verze stejného herního modelu se spouští `java -cp build/check Playtest 17`. Číslo je semínko náhody. Popis příkazů se vypíše při spuštění; `end` vyhodnotí aktuální plán. Pro vývoj existuje také `BalanceSimulation`, jeho jednoduché strategie ale nepředstavují optimální lidské hraní.

Podrobný **anglický** záznam rozhodnutí a jejich změn je v `.leave/progress.txt`, aktuálně sekce 16. Adresář `.leave` je podle stávajícího `.gitignore` pouze lokální. Starší sekce jsou historické a nejsou aktuální specifikací hry.
