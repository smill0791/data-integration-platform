import { LightningElement, wire, track } from 'lwc';
import getRecentSyncJobs from '@salesforce/apex/DataPlatformController.getRecentSyncJobs';
import triggerSync from '@salesforce/apex/DataPlatformController.triggerSync';
import { refreshApex } from '@salesforce/apex';

const STATUS_STYLES = {
    COMPLETED: 'slds-badge slds-theme_success',
    FAILED: 'slds-badge slds-theme_error',
    RUNNING: 'slds-badge slds-badge_inverse',
    QUEUED: 'slds-badge'
};

const SOURCE_STYLES = {
    CRM: 'slds-badge',
    ERP: 'slds-badge',
    ACCOUNTING: 'slds-badge',
    SALESFORCE: 'slds-badge slds-badge_inverse'
};

export default class SyncDashboard extends LightningElement {
    @track jobs = [];
    @track syncMessage = '';
    @track isSyncing = false;
    isLoading = true;
    wiredJobsResult;
    refreshInterval;

    @wire(getRecentSyncJobs)
    wiredJobs(result) {
        this.wiredJobsResult = result;
        this.isLoading = false;
        if (result.data) {
            this.jobs = result.data.map(job => ({
                ...job,
                recordsProcessed: job.recordsProcessed || 0,
                recordsFailed: job.recordsFailed || 0,
                formattedStartTime: job.startTime
                    ? new Date(job.startTime).toLocaleString()
                    : '-',
                statusBadgeClass: STATUS_STYLES[job.status] || 'slds-badge',
                sourceBadgeClass: SOURCE_STYLES[job.sourceName] || 'slds-badge'
            }));

            // Auto-refresh while any job is running or queued
            const hasActiveJob = this.jobs.some(
                j => j.status === 'RUNNING' || j.status === 'QUEUED'
            );
            if (hasActiveJob && !this.refreshInterval) {
                this.startPolling();
            } else if (!hasActiveJob && this.refreshInterval) {
                this.stopPolling();
            }
        } else if (result.error) {
            this.syncMessage = 'Error loading jobs: ' + this.reduceErrors(result.error);
        }
    }

    get hasJobs() {
        return this.jobs && this.jobs.length > 0;
    }

    get syncMessageClass() {
        if (this.syncMessage && this.syncMessage.includes('Error')) {
            return 'slds-notify slds-notify_alert slds-theme_error slds-var-m-bottom_small';
        }
        return 'slds-notify slds-notify_alert slds-theme_info slds-var-m-bottom_small';
    }

    async handleTriggerSync() {
        this.isSyncing = true;
        this.syncMessage = '';
        try {
            const result = await triggerSync();
            this.syncMessage = `Sync job ${result.id} queued successfully!`;
            await refreshApex(this.wiredJobsResult);
            this.startPolling();
        } catch (error) {
            this.syncMessage = 'Error triggering sync: ' + this.reduceErrors(error);
        } finally {
            this.isSyncing = false;
        }
    }

    startPolling() {
        if (this.refreshInterval) return;
        this.refreshInterval = setInterval(() => {
            refreshApex(this.wiredJobsResult);
        }, 5000);
    }

    stopPolling() {
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
            this.refreshInterval = null;
        }
    }

    disconnectedCallback() {
        this.stopPolling();
    }

    reduceErrors(errors) {
        if (!Array.isArray(errors)) {
            errors = [errors];
        }
        return errors
            .filter(error => !!error)
            .map(error => {
                if (typeof error === 'string') return error;
                if (error.body && error.body.message) return error.body.message;
                if (error.message) return error.message;
                return JSON.stringify(error);
            })
            .join(', ');
    }
}
