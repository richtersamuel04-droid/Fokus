package de.fokus.app;

import java.time.LocalDate;

/** Täglich wechselnde Inhalte: Farben, Sprüche, Challenges. */
final class Content {
    private Content() {}

    static final String[] COLOR_NAMES = {"Ozean", "Minze", "Lila", "Koralle", "Gold", "Pink"};
    static final int[] COLORS = {0xFF4FC3F7, 0xFF4DD0A8, 0xFFB388FF, 0xFFFF8A65, 0xFFFFD54F, 0xFFF06292};

    static final String[] QUOTES = {
            "Kleine Schritte jeden Tag schlagen große Pläne, die nie starten.",
            "Fokus ist kein Talent. Fokus ist eine Entscheidung – immer wieder neu.",
            "Das Handy kann warten. Dein Ziel nicht.",
            "Erst die Arbeit, dann der Scroll. Oder einfach gar kein Scroll.",
            "Wer heute anfängt, ist morgen schon einen Tag weiter.",
            "Langeweile ist der Moment, in dem gute Ideen entstehen.",
            "Du musst nicht motiviert sein. Du musst nur anfangen.",
            "Jedes Reel, das du nicht schaust, ist Zeit für dein eigenes Ding.",
            "Disziplin ist, sich daran zu erinnern, was man wirklich will.",
            "Mach es fertig, nicht perfekt.",
            "Eine Sache nach der anderen. Das reicht völlig.",
            "Deine Aufmerksamkeit ist das Wertvollste, was du hast. Verschenk sie nicht.",
            "Heute ist ein guter Tag, um etwas Echtes zu bauen.",
            "Wer seine Zeit kontrolliert, kontrolliert sein Leben.",
            "Fang mit der schwersten Aufgabe an – danach ist alles leichter.",
            "Pause heißt Pause. Nicht Handy.",
            "Konzentration ist ein Muskel. Du trainierst ihn gerade.",
            "Schau nicht, was andere machen. Mach dein Ding.",
            "Zehn Minuten Fokus sind mehr wert als eine Stunde Halbherzigkeit.",
            "Aus Gewohnheiten werden Ergebnisse.",
            "Der beste Moment, das Handy wegzulegen, ist jetzt.",
    };

    static final String[] CHALLENGES = {
            "25 Minuten am Stück arbeiten – Handy in einen anderen Raum.",
            "Heute Mittag eine Pause komplett ohne Handy.",
            "Vor dem Frühstück kein Social Media.",
            "Eine Aufgabe erledigen, die du schon lange vor dir herschiebst.",
            "10 Liegestütze, sobald du das Handy ohne Grund in die Hand nimmst.",
            "Heute Abend eine Stunde vor dem Schlafen Handy weg.",
            "Einer Person eine echte Nachricht schreiben statt nur zu liken.",
            "15 Minuten raus an die frische Luft – ohne Kopfhörer.",
            "Deinen Arbeitsplatz für morgen vorbereiten und aufräumen.",
            "Drei Dinge aufschreiben, die heute gut gelaufen sind.",
            "Eine Stunde konzentriert an der Firma arbeiten.",
            "Benachrichtigungen einer nervigen App ausschalten.",
            "Etwas Neues lernen: 20 Minuten Fachbuch, Video oder Kurs.",
            "Beim Essen kein Handy auf dem Tisch.",
            "Eine Sache reparieren oder fertig machen, die rumliegt.",
            "Zwei Stunden kein YouTube und kein Instagram.",
            "Morgen früh die wichtigste Aufgabe zuerst – heute schon festlegen.",
            "Jemandem aus der Familie bei etwas helfen.",
            "Einmal richtig Sport machen – egal wie kurz.",
            "Den Tag ohne einen einzigen geblockten Reels-Versuch beenden.",
    };

    private static int day() {
        return LocalDate.now().getDayOfYear() + LocalDate.now().getYear() * 3;
    }

    static String quote() {
        return QUOTES[day() % QUOTES.length];
    }

    static String challenge() {
        return CHALLENGES[(day() * 7) % CHALLENGES.length];
    }

    static int autoColorIndex() {
        return day() % COLORS.length;
    }
}
