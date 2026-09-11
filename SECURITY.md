# Security

## Scope and Limitations

This actor is designed as **administrative support only**. It does NOT handle
clinical decisions, medications, or treatment determinations. Any implementation
must maintain strict separation between this actor and clinical systems.

## Reporting a Vulnerability

If you discover a security vulnerability, please report it privately to the
maintainers via GitHub security advisory or email through the cloud-itonami
organization, rather than opening a public issue.

Include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

## Audit and Compliance

- All proposals are logged to an append-only audit ledger
- No record is mutated in place (immutable append-only semantics)
- All administrative actions require explicit governor approval before execution
- Incident flagging always requires human review and sign-off

## Testing

Run the full test suite before deployment:

```bash
kbb -M:test
```

Tests verify governor invariants, store immutability, and actor correctness.
