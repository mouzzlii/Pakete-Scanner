# TRAKUNA – Carrier API research (Austria)

**Research snapshot:** 27 November 2026  
**Purpose:** identify official integration paths and prerequisites. This is planning evidence, not proof of API access or a carrier contract.

## Critical distinction: shipping APIs are not parcel-shop partner APIs

An API for creating labels, booking outgoing shipments or tracking a sender's own consignments does not automatically authorize or support:
- accepting parcels on behalf of a carrier at a partner shop,
- scanning inbound handovers or driver manifests,
- releasing parcels to recipients,
- cash-on-delivery settlement,
- shop partner billing, daily closing or proof-of-handover.

TRAKUNA must confirm the specific parcel-shop/partner agreement and operational API for each carrier. Do not treat a general shipping API as proof of parcel-shop capability.

## Provider findings

### Österreichische Post
- Official Post.at API documentation lists tracking data, post branch information, pickup-order creation and return-label creation.
- The documentation says API access must be requested through the sales representative.
- The separate Post Business Solutions Developer Portal primarily describes document processing, dual delivery and e-signature products; do not confuse it with the parcel API.
- Next step: obtain the exact API product, scope, sandbox, authentication, rate limits and partner-shop functions from Österreichische Post.

Sources:
- https://customerservices.post.at/
- https://www.post.at/g/c/developer-portal

### DPD
- DPD's official developer area describes web services for label printing, tracking and Pickup parcel-shop lookup, plus a tracking push service.
- Developer registration is required to access the full technical information and sandbox.
- DPD documentation describes SOAP services and also REST endpoints for selected functions; the shipment service may have different availability from tracking or shop-finder services.
- DPD Austria's consumer tracking page describes 14-digit parcel numbers and a separate PIN format. Those are different identifiers; a barcode pattern alone is not proof of a carrier or shop authorization.
- Next step: confirm Austrian DPD Cloud/Web Connect product, sandbox credentials, production certification and the specific Pickup parcel-shop partner workflow.

Sources:
- https://www.dpd.com/de/de/entwickler-integration-in-ihre-versandloesung/
- https://esolutions.dpd.com/dokumente/DPD_Cloud_Service_Webservice_Dokumentation_DE.pdf
- https://esolutions.dpd.com/dokumente/API_Developer_Guidelines.pdf
- https://www.dpd.com/at/de/faq/wo-ist-mein-paket-wie-kann-ich-mein-paket-verfolgen/

### GLS Austria
- YourGLS is the browser-based business portal for lower/irregular volumes and supports manual label creation, data import, pickup planning, returns and user/rights management.
- GLS describes ShipIT hosted by GLS as a professional solution for regular consignors above 30 parcels/day, with a REST/SOAP API and label outputs including PDF, PNG and native ZPL.
- GLS states that individual login data is supplied by GLS for API/plugin usage.
- Next step: confirm whether a GLS partner-shop receiving/handout interface exists for the intended shop contract; ShipIT is described as a shipping system and must not be assumed to cover shop counter operations.

Sources:
- https://gls-group.com/AT/en/sending-parcel/shiping-information/dispatch-system/
- https://gls-group.com/AT/de/pakete-versenden/versandinfos/

### UPS
- UPS provides an official developer portal and OAuth 2.0 integration guidance.
- Its developer kit lists shipment, rating, tracking and pickup-related API functions; some legacy documentation requires login.
- Next step: confirm the applicable Austrian account type, production approval, sandbox scope, API products and any parcel-shop partner workflow with UPS.

Sources:
- https://developer.ups.com/?loc=de_AT
- https://developer.ups.com/upsdeveloperkit?loc=en_US

### DHL
- DHL Austria describes multiple divisions and customer portals. DHL Express (MyDHL+) and DHL eCommerce/eConnect are different products; do not treat them as one API.
- DHL eConnect Europe covers Parcel Connect / Parcel Return Connect, shipment data, labels and tracking for large e-commerce customers in Europe. Eligibility and service scope depend on the DHL division, country and customer contract.
- The DHL Parcel EU API page for Belgium/Luxembourg/Netherlands is not an Austria parcel API. DHL Freight APIs are freight products and must not be confused with parcel-shop operations.
- Next step: determine whether the target shop's DHL account belongs to Express or eCommerce and request the Austria-eligible product and partner-shop scope.

Sources:
- https://www.dhl.com/at-en/home/dhl-for-your-business/why-partner-with-us/integrations-and-tools.html
- https://developer.dhl.com/api-reference/ecommerce-europe
- https://developer.dhl.com/getting-started/find-the-right-api

### Hermes
- Hermes' official business FAQ says the legacy ProPS API has reached end of life and is being replaced by the Hermes Shipping Interface (HSI), described as REST-based with updated authentication.
- The published FAQ is on the German Hermes business site. It does not prove that the same interface, account eligibility or parcel-shop workflows are available under an Austrian Hermes contract.
- Next step: obtain written confirmation of Austria availability, partner-shop API, test credentials, label/return scope and support lifecycle before implementing.

Source:
- https://www.myhermes.de/geschaeftskunden/faq-umstellung-hsi/

## Recommended integration architecture

1. Keep the current provider-neutral parcel lifecycle independent from carrier APIs.
2. Add separate capability interfaces only when an actual carrier product is confirmed: tracking, inbound shop acceptance, handover/release, returns, label creation, pickup/collection, settlement.
3. Put all carrier credentials and OAuth client secrets on a protected backend, never in the Android APK or source repository.
4. Use per-provider sandbox environments and test credentials; production activation must be explicit and separate.
5. Verify signatures and replay protection for callbacks/webhooks; apply rate limits, idempotency keys, request correlation IDs and audit events.
6. Keep manual fallback visible when a carrier API is unavailable; never report an operation as accepted by a carrier unless the carrier confirms it.
7. Do not implement carrier credentials, contract-specific endpoints or live booking until the shop's legal/contractual authorization is confirmed.

## Evidence boundary
This research establishes publicly described products and prerequisites only. It does not establish that TRAKUNA has credentials, contracts, sandbox accounts, production approval or carrier-side parcel-shop authorization.
