# Search, Filtering & Pagination

Status: owner-confirmed design direction; candidate, not executable contract.

Source: A-003 D01–D14, D17–18; filtering-matching-sorting.md; searchable-field-catalog.md

No query: authorized directory; explicit type Person/Company and lifecycle Active/Inactive/Archived. Sort name/created/updated ASC/DESC; default updatedAt DESC, customerId ASC. Name normalization proposed: Persian/Arabic yeh/kaf equivalence, permitted digit variants, English case-insensitive, trim and collapse name spaces. Official identifiers and phones require approved jurisdiction-specific rules; no transliteration. Opaque cursor binds Tenant, authorization context, filters, sorting; counts exclude inaccessible rows. Snapshot, expiry, exact collation/null order and freshness thresholds OPEN.

| Decision | Confirmed design direction |
|---|---|
| K04-D01 | Authorized field allowlist |
| K04-D02 | Exact identifier/phone/email |
| K04-D03 | Exact/prefix/contains name |
| K04-D04 | AND between fields OR within values |
| K04-D05 | ACTIVE default |
| K04-D06 | Persian/English normalization |
| K04-D07 | Preserve original data |
| K04-D08 | Last-change default sort |
| K04-D09 | Customer ID tie-break |
| K04-D10 | Cursor pagination |
| K04-D11 | 20/50/100 default 20 |
| K04-D12 | Authorized matches/count only |
| K04-D13 | Context-bound validated cursor |
| K04-D14 | No fuzzy/intelligent search |
| K04-D15 | Engine independence; OD-018 OPEN |

