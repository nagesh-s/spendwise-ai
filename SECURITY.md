# Security policy

## Reporting a vulnerability

Please use GitHub's **Security → Report a vulnerability** flow for this repository. The repository owner must enable private vulnerability reporting in GitHub's security settings after creating the repository.

Until that is available, contact the maintainer privately through the contact information on their public GitHub profile. Do not submit exploit details, private keys, account numbers, raw financial SMS, or exported databases in a public issue.

Include a synthetic reproduction, affected version, Android version, impact, and suggested remediation where possible. There is currently no formal response-time or bounty commitment.

## Secret hygiene

Never commit `.env` files, signing keystores, `key.properties`, service credentials, real financial exports, or private user data. If a secret is committed, rotate it before removing or rewriting its Git history; deleting the visible file alone is insufficient.

The project can be scanned with Gitleaks:

```bash
gitleaks git --log-opts='--all' --redact .
```

The repository-local allowlist contains one narrowly scoped exception for the verified SQL identifier `correlationKey`: it applies only to the original commit, exact source/documentation paths, and exact identifier. It must not be broadened to suppress genuine credentials.
