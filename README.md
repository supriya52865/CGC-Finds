# CGC Finds - Lost & Found

A website where students can report lost and found items, search by category, and get **automatic match suggestions** between lost and found reports using keywords.

## Features
- Report a lost or found item (name, category, description, place, date, contact)
- Search by keyword and filter by category or type (lost / found)
- Automatic match suggestions with a match score and the common keywords
- Mark items as reunited
- Works on phone and desktop, with light and dark mode
- Reports are saved in the browser (localStorage), no server needed

## Run it
Open `index.html` in any browser. No installation needed.

## Project structure
    CGC-Finds/
    ├── index.html        page structure
    ├── css/style.css     design and layout
    ├── js/script.js      reports, search, keyword matching, saving
    └── java-console/     the same project as a Java console program

## How matching works
1. The item name and description are converted to lowercase keywords. Filler words (the, with, my ...) are removed.
2. Plurals are simplified (`keys` -> `key`) and similar words are treated as one (`mobile` and `smartphone` -> `phone`).
3. A lost report is compared only with found reports, and the other way round. Reunited items are ignored.
4. Score out of 100 = keyword overlap (up to 70) + same category (20) + shared place word (10).
5. Reports with a score of 30 or more are suggested, best first.

## Technologies
HTML, CSS, JavaScript (no libraries). Java 11+ for the console version.
