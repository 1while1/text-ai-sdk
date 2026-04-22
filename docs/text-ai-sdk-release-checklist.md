# Text AI SDK Release Checklist

Use this checklist before publishing `text-ai-sdk` outside the local workspace.

## 1. Pre-release validation

- confirm the README still matches the public API
- confirm the compatibility example suite still compiles and reflects current SDK behavior
- confirm `CHANGELOG.md` includes the current snapshot or release entry
- confirm no sample keys or local-only secrets are present in source or docs

## 2. Test and build verification

Run from `standalone-text-ai-sdk`:

```bash
mvn -q test
mvn -q package
```

Expected results:

- tests pass
- the main jar is generated
- a `-sources.jar` is generated
- a `-javadoc.jar` is generated

## 3. Maintainer-owned metadata to finalize before public publishing

Do not invent these values. Replace them with real project metadata first:

- final release version instead of `1.0.0-SNAPSHOT`
- license selection and license metadata
- SCM URL and connection strings
- developer or organization metadata
- project homepage URL if public publishing requires one

## 4. Artifact sanity checks

Verify in `target`:

- `text-ai-sdk-<version>.jar`
- `text-ai-sdk-<version>-sources.jar`
- `text-ai-sdk-<version>-javadoc.jar`

Also spot check:

- README links point to existing files
- docs do not reference deleted APIs
- `pom.xml` still uses the intended `groupId`, `artifactId`, and Java release level

## 5. Publication decision

Before publishing, confirm:

- the package scope is still pure text
- retry behavior is still intentionally limited to non-stream requests
- compatibility examples reflect the provider and gateway scenarios you want to support publicly
- follow-up capability packages such as tool calling or multimodal support are not being advertised by this module

## 6. Optional release follow-ups

After a successful local package build, consider adding:

- signed artifacts if publishing to Maven Central
- CI packaging workflow
- release notes generated from `CHANGELOG.md`
- semantic versioning policy for future package family modules
