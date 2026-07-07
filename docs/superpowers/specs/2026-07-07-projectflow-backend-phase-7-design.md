# ProjectFlow Backend Phase 7 Design

## Overview

Phase 7 strengthens the attachment workflow for frontend integration. Earlier phases already provide a storage abstraction, local storage, Aliyun OSS boundary, file metadata, basic upload, list, download, and delete APIs. The current file APIs work, but they are too permissive for daily use: upload constraints are minimal, version numbers must be manually supplied, list requests can accidentally return broad data, and file write operations do not yet have RBAC action permissions.

This phase keeps the storage abstraction unchanged and focuses on predictable API behavior.

## Goals

- Enforce a 50 MB upload limit.
- Restrict uploads to the first supported document and image formats.
- Generate the next version number when the client does not provide one.
- Require `businessType` and `businessId` when listing files.
- Keep downloads authenticated-only.
- Protect upload and delete operations with file action permissions.
- Seed file permissions for the admin role.

## Upload Validation

Allowed file extensions:

- `docx`
- `xlsx`
- `pdf`
- `png`
- `jpg`
- `jpeg`
- `drawio`

The validation uses the original file name extension because frontend uploads may send inconsistent content types. Content type is still stored for download responses.

Rules:

- Missing file, empty file, missing `businessType`, or missing `businessId` returns `400`.
- File size greater than 50 MB returns `400`.
- Missing or unsupported extension returns `400`.
- The default fallback file name is no longer used for accepted uploads; a usable original filename is required.

## Version Numbering

The client may still provide `versionNo`.

If `versionNo` is blank:

1. Query existing non-deleted files for the same `businessType`, `businessId`, and `originalName`.
2. Find the largest `vN` version.
3. Assign the next version, starting from `v1`.

Examples:

- No existing file: `v1`.
- Existing `v1`: next is `v2`.
- Existing `v1`, `v3`, and `draft`: next is `v4`.

This keeps the frontend simple while preserving manual version labels when explicitly supplied.

## Listing

`GET /api/files` must require both `businessType` and `businessId`.

This prevents accidental broad file listing and matches how frontend pages use attachments: every attachment panel belongs to one project, task, requirement, or BUG.

The response remains `List<FileResponse>` ordered by latest upload first.

## Deletion

Delete keeps the existing logical delete behavior through MyBatis-Plus `@TableLogic`. The physical object is still deleted from storage after metadata deletion to avoid orphaned local files in daily development.

Phase 7 does not add recycle-bin restore.

## Permissions

Add file permissions:

- `file:upload`
- `file:delete`

Apply permissions:

- Upload requires `file:upload`.
- Delete requires `file:delete`.
- List and download remain authenticated-only.

Add a `file` menu root and assign all file permissions to the seeded admin role.

## Error Handling

Use existing `BusinessException` and `ErrorCode.BAD_REQUEST` for validation failures.

User-facing messages should be specific enough for frontend debugging:

- `Invalid file upload request`
- `File name is required`
- `File size exceeds 50 MB`
- `File type not allowed`
- `File query requires businessType and businessId`

## Testing

Service tests cover:

- Upload rejects unsupported extension.
- Upload rejects files over 50 MB.
- Upload auto-generates `v1`.
- Upload auto-generates the next version after existing `v1` and `v3`.
- List rejects missing business object filters.
- Existing upload persistence behavior still works.

Security tests cover:

- Upload without `file:upload` returns `403`.
- Delete without `file:delete` returns `403`.
- File list remains authenticated-only.

Startup verification covers:

- Flyway validates and applies `V7__phase_7_file_permissions.sql`.
- OpenAPI starts after file permission annotations are added.

## Out of Scope

Phase 7 does not implement:

- File preview.
- Virus scanning.
- MIME sniffing.
- MinIO implementation.
- Signed OSS download URLs.
- Batch upload.
- Restore after delete.
- Data-level authorization by business object owner.

## Definition of Done

- Upload validation enforces size and extension rules.
- Blank version numbers are generated automatically.
- File list requires one business object.
- File upload and delete are protected by RBAC permissions.
- Admin seed data includes file permissions.
- Full tests, package build, and local OpenAPI startup verification pass.
