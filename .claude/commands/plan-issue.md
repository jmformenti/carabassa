---
description: Draft an implementation plan for a GitHub issue and, once you approve it, hand the issue to the autonomous implementer
argument-hint: <issue number>
---

Prepare issue #$ARGUMENTS of this repository for the autonomous implementation flow
(patufet, see `.github/workflows/patufet.yml`). Follow these steps:

1. Read the issue with `gh issue view $ARGUMENTS --comments`.
2. Explore the relevant code to understand how the requested change fits (follow the
   repository conventions: CLAUDE.md, CONTRIBUTING, README).
3. Write a concrete implementation plan: files to touch, API/data changes if any, and how it
   will be verified (which tests, which build). **Do not implement anything yet.**
4. Show me the plan and iterate on it with me until I explicitly approve it.
5. Only when I confirm:
   - Post the plan as an issue comment with `gh issue comment $ARGUMENTS --body-file <file>`.
     The comment must start with the line `<!-- patufet:plan -->` followed by a heading
     (e.g. `## Implementation plan`).
   - Mark the issue as ready with `gh issue edit $ARGUMENTS --add-label <ready label>`, where
     the ready label is the `label-ready` value of the `implement` job in
     `.github/workflows/patufet.yml`, or `ready-to-implement` if it is not set. This label
     starts the automatic implementation immediately, so never add it without my explicit
     confirmation.
