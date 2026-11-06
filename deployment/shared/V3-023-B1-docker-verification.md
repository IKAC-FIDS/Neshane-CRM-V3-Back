# V3-023 Stage B1 — Docker Verification Evidence

## Scope

- Stage: B1
- Mode: REFERENCE_ONLY
- Environment: Disposable Docker Compose reference deployment
- Status: Engineering verification passed for the scenarios below
- Production authorization: Not granted

## Runtime

- Image: neshane-v3-023-reference-runtime:local
- Services: reference-gateway, cell-a-peer, cell-b-peer
- Host ingress: 127.0.0.1:18080 -> reference-gateway:8080
- Gateway network membership: ingress-network, cell-a-network, cell-b-network
- Cell A Peer: cell-a-network only
- Cell B Peer: cell-b-network only
- Cell networks: internal Docker bridge networks
- Reference contracts and fixtures: read-only mounts
- Runtime user: 10001:10001

## End-to-End HTTP Results

| Request | Observed response | Result |
| --- | --- | --- |
| POST /tenant-a | 200 cell-a-ok | PASS |
| POST /tenant-b | 200 cell-b-ok | PASS |
| POST /cross-a | 403 denied | PASS |
| POST /cross-b | 403 denied | PASS |
| POST /unknown | 403 denied | PASS |
| POST /not-registered | 404 not-found | PASS |

## Forwarding Gate Evidence

Before denied requests:
- Cell A calls: 2
- Cell B calls: 1

After POST /cross-a, POST /cross-b, and POST /unknown:
- Cell A calls: 2
- Cell B calls: 1

Result: PASS. Denied requests did not increment either Peer counter.

## Network Isolation Evidence

Observed Cell IPs:
- Cell A: 172.30.1.2
- Cell B: 172.30.2.2

TCP port: 8080

| Connection | Observed | Result |
| --- | --- | --- |
| Cell A -> itself | Connected | PASS |
| Cell B -> itself | Connected | PASS |
| Cell A -> Cell B IP | Not connected | PASS |
| Cell B -> Cell A IP | Not connected | PASS |

Each Peer was observed only on its dedicated Docker network.
Positive self-connectivity controls succeeded before cross-cell tests.

## Limitations

- Synthetic tenants, static fixtures, and controlled reference trust only.
- HTTP paths simulate trusted tenant selection; no production authentication.
- No production-grade cryptographic snapshot verification.
- No proof of tenant data isolation or PostgreSQL RLS.
- TCP tests cover the observed Peer IPs and port 8080 only.
- The Gateway is connected to both Cell networks.
- Network test outcomes do not establish resistance to every bypass path.
- Docker Image security scanning and production hardening are not established.
- No production deployment, merge, or contract publication is authorized.

## Conclusion

The tested reference routing, denied-request non-forwarding,
and direct Peer-to-Peer TCP isolation scenarios passed.

Stage B1 engineering evidence is recorded.
Formal acceptance and any subsequent stage authorization remain separate.
