# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Three roles, drawn from `docs/requirements.md`, section 2:

- **Paciente.** Requests home-care attention for themselves or a family member.
  A meaningful share of this audience is older adults, which is a named design
  constraint (`.claude/rules/compose.md`), not an assumption.
- **Profesional.** An independent doctor, nurse, physiotherapist, or health
  sciences student who provides home-care attention outside any clinic or
  institution, and depends on their own individual promotion to find work today.
- **Administrador.** Verifies professionals' documents and reconciles payments
  and platform commissions. Never accesses conversation content between patient
  and professional (INV-12).

## Product Purpose

Salud en Casa connects patients with home-care health professionals in Bolivia.
It replaces the informal channels this coordination runs through today —
personal recommendations, social media posts, instant-messaging conversations —
with a system that gives the patient standardized information to decide with,
keeps a record of how each attention was coordinated, and gives the independent
professional a channel to reach work that does not depend solely on personal
promotion.

## Positioning

The product's mechanism, stated directly in `docs/requirements.md` section 1:
geographic-proximity matching, rate negotiation between the two parties, a kept
record of the attention, and verifiable reputation built from completed
services. None of the three problems it targets are solved by the channels
patients and professionals use today (informal recommendations, social media,
messaging apps): those channels give the patient no standardized information to
decide with, leave no record of how a specific attention was coordinated, and
give the professional no way to build reputation that outlives one personal
contact.

## Operating Context

- **Degree project under SCRUM.** Development is incremental: each sprint ships
  a demonstrable vertical slice, never a horizontal layer. `plan.md` is the
  sprint plan; the finished product is evaluated by an academic tribunal, which
  makes the documented decision trail (`docs/decisions.md`) and the
  entity-relationship diagram (`docs/architecture/data-model.md`) part of the
  deliverable, not incidental artifacts.
- **Backend is Supabase, worked against the remote project directly** —
  PostgreSQL 17 with PostGIS, Auth, Realtime, Storage — with no local Docker
  environment in this phase. Every table carries row level security from the
  migration that creates it; RLS is the actual access boundary, not a
  client-side check (INV-02, INV-13).
- **Proximity search always resolves inside the database with PostGIS**, never
  through an external proximity service and never by filtering distances on the
  client (INV-08), because the app is deployed in Bolivia and an external
  location-search service is a cost this budget does not carry.
- **Role permissions are asymmetric per operation**, not just per screen — the
  full matrix is `docs/requirements.md` section 7. The one invariant worth
  naming here because it shapes the whole messaging feature: the administrator
  role never reads `messages`, under any operation (INV-12).

## Capabilities and Constraints

**Core capabilities (RF-01 through RF-13, `docs/requirements.md`):** Google
sign-in with role selection (patient/professional, assigned once, never
self-assignable as admin); profile and professional-service management;
address geocoding with a primary address; document-based professional
verification before appearing in search; proximity search filtered by service
type and availability, ordered by distance; immediate or scheduled attention
requests; rate negotiation as an append-only offer/counteroffer thread; a
conversation attached to each request; a service lifecycle (assigned → in
progress → completed, or cancelled with a reason); a payment lifecycle that
closes only when patient and professional confirm separately; bidirectional
1–5 ratings with comment, one per service per author; and a history view per
role.

**Non-functional constraints (RNF-01 through RNF-10):**
- Proximity search responds in under one second with 500+ professionals loaded.
- Cold start under two seconds on a mid-range device.
- App package under 25 MB.
- A user's data is never accessible to another user, enforced by a database
  policy verified with an automated test, not by application logic.
- Data travels and rests encrypted.
- No credential ever appears in source code or version control (RNF-06,
  INV-14: the mobile client uses only the anonymous key; the service key never
  leaves the server environment).
- The interface meets accessibility contrast criteria and responds to the
  system's font-size setting.
- Every list is paginated; no query returns an unbounded collection.
- The domain layer has no Android dependency and its tests run without an
  emulator.
- Monthly infrastructure cost stays under USD 35 in early operation.

**Explicitly out of scope this phase** (`docs/requirements.md` section 9, and
`CLAUDE.md`): any multi-platform target or shared cross-platform code — the
product is Android-only by deliberate scope decision, not a temporary
limitation; a local offline database (DataStore covers session and preferences
only); a structured clinical record (the attention's detail lives in the
conversation for now); and a payment gateway integration beyond cash and
direct QR confirmation.

## Brand Commitments

- **Name:** Salud en Casa (`app_name`, both locale resource files). Written in
  Spanish; it is not translated in the English string resources.
- **Visual identity is already specified**, independently of this file:
  `docs/design-system.md` derives the palette (teal `#1A6F8F` primary, with two
  documented contrast corrections — amber `#FFA600` is fill-only, never text;
  the status green darkens to `#15782B` when it carries text) and the Inter
  typeface from the reference kit at `docs/design/references/`. This file does
  not restate or supersede that specification.
- **No stock photography of people ships in the product.** The reference kit's
  photography is reference material only (`docs/design/references/README.md`);
  a user's own profile photo comes from their account or an initials marker.
  The reference kit's medical-specialty icons are likewise not reused as-is;
  they are replaced with Material Symbols or original illustrations.
- **Initial language is Spanish** (`values-es/`), with `values/` as the
  fallback locale structure so English becomes a translation pass later, not a
  restructuring (`.claude/rules/i18n.md`).

## Evidence on Hand

None yet. This is a degree project without real patients or professionals
signed up, and without a pilot, testimonials, or production usage data —
confirmed directly rather than assumed. Nothing in future design or copy work
should imply real users, real reviews, or real service history until this
changes.

## Product Principles

1. **Verifiable trust replaces unverified word of mouth.** A professional
   never appears in search until their documents are approved (INV-07); this
   is the product's actual differentiator, not a compliance afterthought.
2. **The server is the only enforcement boundary.** Proximity, role
   permissions, and per-row data access are never trusted to the client — they
   are PostGIS queries and row level security policies, verified against the
   real database rather than assumed correct because the code reads correctly.
3. **Design for the user who is actually there, including older adults.**
   Contrast, font-scaling response, and 48 dp touch targets are constraints on
   every screen, not a pass applied at the end.
4. **Ship one demonstrable vertical slice at a time.** A sprint's increment is
   a working piece of the product end to end, never a layer that only becomes
   visible once another layer is built on top of it.
5. **Minimal footprint by default.** No dependency, abstraction, or library
   enters the project without a requirement that names it; package size and
   monthly infrastructure cost are both fixed constraints, not targets to
   revisit later.

## Accessibility & Inclusion

RNF-07 requires meeting accessibility contrast criteria and responding
correctly to the system's font-size setting, verified up to 200% in practice
(`.claude/rules/compose.md`) because part of the user base is older adults —
named explicitly in the requirements, not inferred. Every tappable element is
at least 48 dp. Both light and dark schemes must render correctly. The palette
in `docs/design-system.md` already carries two corrections made specifically
for this reason (see Brand Commitments above).
