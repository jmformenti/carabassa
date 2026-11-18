---
description: Draft an implementation plan for a GitHub issue and, once you approve it, hand the issue to the autonomous implementer
argument-hint: <issue number>
---

Prepare issue #$ARGUMENTS of this repository for the autonomous implementation flow
(patufet, see `.github/workflows/patufet.yml`). The implementer follows the approved plan and
does not re-plan: any decision the plan leaves open stops it with a question. Your job is to
leave none open. Follow these steps:

1. Read the issue with `gh issue view $ARGUMENTS --comments`. If it already has a comment
   containing `<!-- patufet:plan -->` written by an owner, member or collaborator of the
   repository, this is a re-run. The latest such comment is the current plan, the one the
   implementer follows, and the comments after it (usually a question from the implementer)
   are what this run must resolve. A marked comment from anyone else is not a plan.
2. Explore the relevant code to understand how the requested change fits (follow the
   repository conventions: CLAUDE.md, CONTRIBUTING, README).
3. Settle the open decisions with me before writing the plan:
   - Separate facts from decisions. Facts (what the code does, which tests exist, which
     conventions apply) are yours to find: look them up, with a sub-agent if it helps, and
     never ask me for something you can read. Decisions (scope, behaviour, trade-offs) are
     mine.
   - If neither the issue nor the code leaves a decision open, say so in one line and go to
     step 4.
   - Otherwise ask in rounds. A round holds every open decision that does not depend on
     another decision still open; the dependent ones wait for a later round. Number each
     question, give your recommended answer and why, and word it so that "yes" accepts the
     recommendation. Wait for my answers, then work out the next round.
   - Stop when no decision is open and nothing is assumed without saying so.
   - On a re-run, start from the implementer's question and the decisions that follow from
     it. Do not reopen what the previous plan already decided.
4. Write a concrete implementation plan: files to touch, API/data changes if any, how it will
   be verified (which tests, which build), and a `### Decisions` section listing each decision
   settled in step 3 with its reason. On a re-run, write the whole plan again: the
   implementer reads only the latest plan, so keep the scope and the decisions of the
   previous one that still hold and change only what this run decided.
   **Do not implement anything yet.**
5. Show me the plan and iterate on it with me until I explicitly approve it.
6. Only when I confirm:
   - Post the plan as an issue comment with `gh issue comment $ARGUMENTS --body-file <file>`.
     The comment must start with the line `<!-- patufet:plan -->` followed by a heading
     (e.g. `## Implementation plan`), and contain only the plan.
   - Mark the issue as ready with `gh issue edit $ARGUMENTS --add-label <ready label>`, where
     the ready label is the `label-ready` value of the `implement` job in
     `.github/workflows/patufet.yml`, or `ready-to-implement` if it is not set. This label
     starts the automatic implementation immediately, so never add it without my explicit
     confirmation.

<!-- Step 3 adapts the method of the "grilling" skill by Matt Pocock
     (https://github.com/mattpocock/skills, MIT). -->
