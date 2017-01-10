import requests
from urllib import quote
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.events import EVENT_JOB_ERROR, EVENT_JOB_EXECUTED
import time
import logging
import Queue

source = 'http://localhost:4301'
target = 'http://localhost:3030'
ds = 'ds'
context = 'provenance'

q = Queue.Queue()

FORMAT = '[%(asctime)s] [%(process)d] [%(levelname)s] %(message)s'
logging.basicConfig(format=FORMAT)
logger = logging.getLogger('tcpserver')


def cronJob():
    """Simple job that gets workflows and activities and uploads."""
    workflow = getResult(source, 'workflow')
    activity = getResult(source,  'activity')
    clean(target, ds, context)
    postResult(target, workflow, ds, context)
    postResult(target, activity, ds, context)


def getResult(source, endpoint, modifiedSince=None):
    """Get data from WF-API from a source."""
    if modifiedSince is None:
        wf_api = "{0}/v0.1/{1}".format(source, endpoint)
    else:
        wf_api = "{0}/v0.1/{1}?modifiedSince={2}".format(source, endpoint,
                                                         quote(modifiedSince))
    result = requests.get(wf_api)
    msg = 'Endpoint {0} with the HTTP Response {1}.'.format(endpoint,
                                                            result.status_code)
    logger.info(msg)
    q.put(msg)
    return result


def postResult(target, data, dataset, context=None):
    """Post data to a target Graph Store."""
    if context is None:
        store_api = "{0}/{1}/data".format(target, dataset)
    else:
        store_api = "{0}/{1}/data?graph={2}".format(target, dataset, context)
    headers = {'Content-Type': 'text/turtle'}
    result = requests.post(store_api, data=data, headers=headers)
    msg = 'Endpoint {0} with the HTTP Response {1}.'.format(target,
                                                            result.status_code)
    logger.info(msg)
    q.put(msg)
    pass


def sparqlUpdate(target, data, dataset, context=None):
    """SPARQL update to avoid blank node duplicates."""
    pass


def clean(target, dataset, context=None):
    """Clean Graph."""
    if context is None:
        store_api = "{0}/{1}/data".format(target, dataset)
    else:
        store_api = "{0}/{1}/data?graph={2}".format(target, dataset, context)
    requests.delete(store_api)
    msg = 'Graph cleaned.'
    logger.info(msg)
    q.put(msg)
    pass


def my_listener(event):
    """Job event listener."""
    while not q.empty():
        get_ = "msg from job '%s': '%s'" % (event.job_id, q.get())
        print get_
    if event.exception:
        print 'The job did not complete.'
    else:
        print 'The job completed :)'


if __name__ == '__main__':
    scheduler = BackgroundScheduler(timezone="UTC")
    scheduler.add_job(cronJob, 'interval', seconds=3)
    scheduler.add_listener(my_listener, EVENT_JOB_EXECUTED | EVENT_JOB_ERROR)
    scheduler.start()
    q.join()
    try:
        # This is here to simulate application activity
        # (which keeps the main thread alive).
        while True:
            time.sleep(2)
    except (KeyboardInterrupt, SystemExit):
        # Not strictly necessary if daemonic mode is enabled
        # but should be done if possible
        scheduler.shutdown()
