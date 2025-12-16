---
agent: Plan
model: GPT-5.2 (Preview) (copilot)
description: 'Write the code implementation plan for a feature based on its specification.'
argument-hint: 'Provide the feature specification.'
---

# Write Code Implementation for a Feature

## Task

Write the code implementation for a prompted feature based on its specification.

Do not include any tests, only provide the code.

## Context

- The specs file [{specId}.spec.md](/docs/specs/{specId}.spec.md) 
- [/.github/instructions](../instructions) - Existing instruction files directory
- [STRUCTURE.md](/docs/STRUCTURE.md) - Contains the technology stack and approved dependencies

## Steps

### Pre-coding steps (planning)

- Read and analyze the specification document to understand the feature requirements.

- Review current project structure and previous related implementations for consistency.

- Plan the code structure and components needed to implement the feature.

### Coding steps
- Create and use an isolated Git branch for the feature (MANDATORY).

  - Ensure the working tree is clean before starting.
  - Create a feature branch named using the spec id, for example:
    - `feature/{specId}` (e.g. `feature/002-flight-management`)
  - Switch to that branch before writing any code.
  - If there are local, unrelated changes, do not proceed until the tree is clean.

- Write the code implementation in appropriate files and directories following project conventions.

- Don not wait for approval or feedback, proceed directly to implementation.

- Commit work in Git (MANDATORY).

  - After implementing the feature, create at least one commit with a descriptive Conventional Commit message, for example:
    - `feat: implement {specId} flight management`
  - Prefer small commits if it helps readability, but avoid excessive commit splitting.

### Post-coding steps

- Update documentation (MANDATORY).

  - Update [STRUCTURE.md](/docs/STRUCTURE.md) to reflect what was implemented.
  - Document:
    - new HTTP contexts/endpoints added (e.g. `/flights`)
    - new handlers/services/repositories/models created
  - If no dependency changes were introduced, explicitly state “No dependency changes” in the relevant section/update note.

- Commit the changes with a descriptive message indicating the feature implemented.

## Validation

- Ensure the code implementation is complete and is syntactically correct
  - [ ] The code builds/compiles without errors
  - [ ] No tests are included, no need testing at this point  
  - [ ] Structure document is updated if necessary
  - [ ] Feature branch is clean and ready for merging