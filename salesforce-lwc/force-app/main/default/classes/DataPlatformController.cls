public with sharing class DataPlatformController {

    // Update this URL to your ngrok or deployed backend URL
    private static final String BACKEND_BASE_URL = 'https://YOUR-NGROK-URL.ngrok-free.app';

    @AuraEnabled(cacheable=true)
    public static List<SyncJobWrapper> getRecentSyncJobs() {
        HttpRequest req = new HttpRequest();
        req.setEndpoint(BACKEND_BASE_URL + '/api/integrations/jobs');
        req.setMethod('GET');
        req.setHeader('Content-Type', 'application/json');
        req.setTimeout(10000);

        Http http = new Http();
        HttpResponse res = http.send(req);

        if (res.getStatusCode() != 200) {
            throw new AuraHandledException('Failed to fetch sync jobs: ' + res.getStatusCode());
        }

        List<SyncJobWrapper> jobs = (List<SyncJobWrapper>) JSON.deserialize(
            res.getBody(), List<SyncJobWrapper>.class
        );
        return jobs;
    }

    @AuraEnabled
    public static SyncJobWrapper triggerSync() {
        HttpRequest req = new HttpRequest();
        req.setEndpoint(BACKEND_BASE_URL + '/api/integrations/sync/salesforce-contacts');
        req.setMethod('POST');
        req.setHeader('Content-Type', 'application/json');
        req.setTimeout(10000);

        Http http = new Http();
        HttpResponse res = http.send(req);

        if (res.getStatusCode() != 202) {
            throw new AuraHandledException('Failed to trigger sync: ' + res.getStatusCode());
        }

        SyncJobWrapper job = (SyncJobWrapper) JSON.deserialize(
            res.getBody(), SyncJobWrapper.class
        );
        return job;
    }

    public class SyncJobWrapper {
        @AuraEnabled public Long id;
        @AuraEnabled public String sourceName;
        @AuraEnabled public String syncType;
        @AuraEnabled public String status;
        @AuraEnabled public Integer recordsProcessed;
        @AuraEnabled public Integer recordsFailed;
        @AuraEnabled public String startTime;
        @AuraEnabled public String endTime;
        @AuraEnabled public String errorMessage;
    }
}
