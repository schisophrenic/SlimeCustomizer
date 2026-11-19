---
name: Startup or gameplay bug
about: Report a SlimeCustomizer-Legacy/GUI issue with the earliest error log and environment.
title: ""
labels: ["bug"]
assignees: []
---

## Version details

- SlimeCustomizer JAR version/commit:
- Paper/Purpur build:
- Minecraft version:
- Java version:
- Slimefun-Legacy version:
- ValhallaMMO-Rewritten version (if installed):

## What happened?

Include what you expected, what happened and exact steps to reproduce.

## Startup diagnostics

- `/sc status` output:
- First `[SC-STARTUP]`, `[SlimeCustomizer]` or `[Slimefun]` error **before** the attempted command:
- Stack trace or attached `latest.log`:

An error saying "plugin is disabled" is not itself the startup cause.

## Relevant configuration

Attach the minimal YAML for the failing item/category/recipe. Remove private
server information, IPs, tokens, passwords and player identifiers.

## Safety checks

- Did you test on a backed-up server?
- Does the issue reproduce after a full restart (not `/reload`)?
- For recipe issues: what changed with item quantities, output and shift-click?
