# Salesforce Developer Org Setup Guide

This guide walks through setting up a Salesforce Developer Org to use as a real data source with the Data Integration Platform.

## 1. Create/Access Developer Org

1. Sign up at https://developer.salesforce.com/signup (free, no credit card)
2. Log in at https://login.salesforce.com
3. You'll receive a Developer Edition org with API access

## 2. Create Sample Contact Data

1. Navigate to **App Launcher** (grid icon, top-left) -> search "Contacts"
2. Create 10-20 sample Contacts with:
   - First Name, Last Name
   - Email
   - Phone
   - Mailing Street, Mailing City, Mailing State, Mailing Zip/Postal Code
3. Alternatively: use **Data Import Wizard** (Setup -> Data Import Wizard) to bulk-load a CSV

## 3. Create a Connected App (for OAuth 2.0)

1. **Setup** -> search "App Manager" -> **New Connected App**
   - (Salesforce also offers the newer **External Client App Manager**; this guide uses the classic Connected App. The **OAuth Usage** page is a monitoring view only — apps are created in App Manager, not there.)
2. Fill in:
   - Connected App Name: `Data Integration Platform`
   - API Name: `Data_Integration_Platform`
   - Contact Email: your email
3. Enable OAuth Settings:
   - Check **Enable OAuth Settings**
   - Callback URL: `https://login.salesforce.com/services/oauth2/success`
     (required field, but the Client Credentials flow never uses it — a placeholder is fine)
   - Selected OAuth Scopes: **Manage user data via APIs (api)** (Full access also works)
4. Save and wait 2-10 minutes for propagation

## 4. Enable the Client Credentials Flow

This project uses the **OAuth 2.0 Client Credentials flow** — a two-legged,
server-to-server grant with **no user login, browser redirect, or security token**.
The backend authenticates as the app itself using only the consumer key and secret
(see `SalesforceAuthService`, which posts `grant_type=client_credentials`).

1. From **App Manager**, find the app -> dropdown -> **Manage** -> **Edit Policies**
2. Under **Client Credentials Flow**, set **Run As** to an execution user. This step is
   required: because no human logs in, Salesforce issues the token in this user's
   context, so that user's profile, permissions, and record access determine what the
   sync can see. Choose a user with API access and visibility of the Contacts to sync.
3. Save. (Newer orgs also expose an **Enable Client Credentials Flow** toggle in the
   app's OAuth settings — make sure it is on.)
4. Back on the app, click **Manage Consumer Details** -> verify your identity ->
   copy the **Consumer Key** and **Consumer Secret**.

> **Interview talking point**: "I use the Client Credentials flow because the sync is a
> backend service with no user present — it runs as a designated Salesforce user, so
> permissions are governed by that user. The trade-off is a shared client secret; in
> production I'd consider JWT Bearer (a key-signed assertion) so no secret crosses the wire."

## 5. Set Environment Variables

```bash
export SF_CLIENT_ID="your_consumer_key"
export SF_CLIENT_SECRET="your_consumer_secret"
# Client Credentials token requests must go to your org's My Domain host,
# NOT the generic login.salesforce.com:
export SF_LOGIN_URL="https://your-domain.my.salesforce.com"
# Optional override (default shown):
# export SF_API_VERSION="v59.0"
```

> **Gotcha**: For the Client Credentials flow, the token endpoint must be your org's
> **My Domain** URL (e.g. `https://mycompany-dev-ed.develop.my.salesforce.com`), not
> `login.salesforce.com`. Requests to the generic login host will be rejected.

## 6. Test the Connection

```bash
curl -X POST "$SF_LOGIN_URL/services/oauth2/token" \
  -d "grant_type=client_credentials" \
  -d "client_id=$SF_CLIENT_ID" \
  -d "client_secret=$SF_CLIENT_SECRET"
```

Response should include `access_token` and `instance_url`.

Then test a SOQL query:

```bash
ACCESS_TOKEN="<from above>"
INSTANCE_URL="<from above>"

curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  "$INSTANCE_URL/services/data/v59.0/query?q=SELECT+Id,FirstName,LastName,Email,Phone+FROM+Contact+LIMIT+5"
```

## 7. Run the Integration

1. Start infrastructure: `docker-compose up -d`
2. Set environment variables (step 5 above)
3. Start all services: `./start-dev.sh`
4. Open http://localhost:3000
5. Click **Salesforce Contacts** sync button
6. Verify job transitions QUEUED -> RUNNING -> COMPLETED
7. Check `[final].customers` table has records with `source_system='SALESFORCE'`

## 8. Lightning Web Component (Optional Demo)

See the `salesforce-lwc/` directory for an SFDX project containing a Sync Dashboard LWC that can be deployed to your Developer Org.

### Prerequisites
- Salesforce CLI (`sf`): `npm install -g @salesforce/cli`
- ngrok for exposing localhost: `brew install ngrok`

### Setup
1. Run `ngrok http 8080` -> copy the public `https://xxxx.ngrok-free.app` URL
2. In Salesforce: **Setup** -> search "Remote Site Settings" -> **New Remote Site**
   - Remote Site Name: `DataPlatformBackend`
   - Remote Site URL: paste ngrok URL
   - Active: checked
3. Update the endpoint URL in `DataPlatformController.cls` or the Custom Setting

### Deploy
```bash
cd salesforce-lwc
sf org login web --alias dev-org
sf project deploy start --target-org dev-org
```

Then add the **Sync Dashboard** component to a Lightning App Page via Lightning App Builder.

> **Note**: ngrok URL changes each session -- update the Remote Site Setting before each demo.

## Architecture Notes

Salesforce Contacts reuse the existing customer pipeline:
- **Staging**: `staging.raw_customers` (same table as CRM)
- **Validated**: `validated.validated_customers`
- **Final**: `[final].customers` with `source_system='SALESFORCE'`

The `SalesforceIntegrationService` normalizes Salesforce Contact JSON into `CrmCustomerResponse` format before staging, so the transformation/validation/loading pipeline is fully source-agnostic.

```
Salesforce API -> SalesforceApiClient (OAuth + SOQL)
                  -> SalesforceIntegrationService (normalize to CRM format)
                     -> staging.raw_customers (same as CRM)
                        -> CustomerTransformationService (source-agnostic)
                           -> CustomerValidationService (source-agnostic)
                              -> CustomerLoadService (parameterized source_system)
                                 -> [final].customers (source_system='SALESFORCE')
```
