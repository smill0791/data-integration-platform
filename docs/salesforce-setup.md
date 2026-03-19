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
2. Fill in:
   - Connected App Name: `Data Integration Platform`
   - API Name: `Data_Integration_Platform`
   - Contact Email: your email
3. Enable OAuth Settings:
   - Check **Enable OAuth Settings**
   - Callback URL: `https://login.salesforce.com/services/oauth2/success`
   - Selected OAuth Scopes: **Full access (full)**, **Perform requests at any time (refresh_token, offline_access)**
4. Save and wait 2-10 minutes for propagation

## 4. Configure OAuth (Username-Password Flow)

The **Username-Password flow** is simplest for a portfolio demo (no browser redirect needed):

1. From the Connected App page, click **Manage Consumer Details** -> copy **Consumer Key** and **Consumer Secret**
2. Get your **Security Token**: Setup -> search "Reset My Security Token" -> click Reset -> check email
3. Your password for the API call = `YOUR_PASSWORD` + `YOUR_SECURITY_TOKEN` (concatenated, no separator)

> **Interview talking point**: "In production I'd use the JWT Bearer or Client Credentials flow for server-to-server auth, but the Username-Password flow is appropriate for this demo context."

## 5. Set Environment Variables

```bash
export SF_CLIENT_ID="your_consumer_key"
export SF_CLIENT_SECRET="your_consumer_secret"
export SF_USERNAME="your_salesforce_username"
export SF_PASSWORD="your_password_plus_security_token"
# Optional overrides (defaults shown):
# export SF_LOGIN_URL="https://login.salesforce.com"
# export SF_API_VERSION="v59.0"
```

## 6. Test the Connection

```bash
curl -X POST https://login.salesforce.com/services/oauth2/token \
  -d "grant_type=password" \
  -d "client_id=$SF_CLIENT_ID" \
  -d "client_secret=$SF_CLIENT_SECRET" \
  -d "username=$SF_USERNAME" \
  -d "password=$SF_PASSWORD"
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
