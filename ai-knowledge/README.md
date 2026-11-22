# ai-knowledge

Short, AI-generated notes about this repository: one card per module, the verified build and test commands, and a log of design decisions and pitfalls. They are plain markdown and need no tools to read.

- `build-and-test.md`: how to build, test and lint, with the commands that were actually run.
- `<module>.md`: what a part of the code does, its key files, entry points and gotchas.
- `decisions.md`: lasting decisions (with the reason a person gave) and mistakes to avoid (with the real error).
- `notes/<topic>.md`: findings that belong to no single module, and descriptions of whole features: integration
  details, feasibility results, workflows. Each note lists the evidence it rests on (code it read, commands it ran,
  documentation it consulted, statements by a person) and any open questions.

Each card and note says when it was checked against the files it lists. The `## Files` and `## Covers` sections carry short content hashes so a script can tell whether the code changed since then; they do not prove the card is correct. Treat the cards as a starting point, check the code before relying on them, and fix or delete a card that is wrong.
