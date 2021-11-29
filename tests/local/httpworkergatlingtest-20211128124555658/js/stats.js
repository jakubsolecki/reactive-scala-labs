var stats = {
    type: "GROUP",
name: "Global Information",
path: "",
pathFormatted: "group_missing-name-b06d1",
stats: {
    "name": "Global Information",
    "numberOfRequests": {
        "total": "30210",
        "ok": "28474",
        "ko": "1736"
    },
    "minResponseTime": {
        "total": "0",
        "ok": "0",
        "ko": "8117"
    },
    "maxResponseTime": {
        "total": "60001",
        "ok": "59988",
        "ko": "60001"
    },
    "meanResponseTime": {
        "total": "1501",
        "ok": "849",
        "ko": "12184"
    },
    "standardDeviation": {
        "total": "4724",
        "ok": "3544",
        "ko": "7834"
    },
    "percentiles1": {
        "total": "223",
        "ok": "3",
        "ko": "10000"
    },
    "percentiles2": {
        "total": "503",
        "ok": "500",
        "ko": "10001"
    },
    "percentiles3": {
        "total": "10000",
        "ok": "2642",
        "ko": "22478"
    },
    "percentiles4": {
        "total": "24244",
        "ok": "17315",
        "ko": "60000"
    },
    "group1": {
    "name": "t < 800 ms",
    "count": 25690,
    "percentage": 85
},
    "group2": {
    "name": "800 ms < t < 1200 ms",
    "count": 0,
    "percentage": 0
},
    "group3": {
    "name": "t > 1200 ms",
    "count": 2784,
    "percentage": 9
},
    "group4": {
    "name": "failed",
    "count": 1736,
    "percentage": 6
},
    "meanNumberOfRequestsPerSecond": {
        "total": "154.923",
        "ok": "146.021",
        "ko": "8.903"
    }
},
contents: {
"req_request-10573": {
        type: "REQUEST",
        name: "request",
path: "request",
pathFormatted: "req_request-10573",
stats: {
    "name": "request",
    "numberOfRequests": {
        "total": "30210",
        "ok": "28474",
        "ko": "1736"
    },
    "minResponseTime": {
        "total": "0",
        "ok": "0",
        "ko": "8117"
    },
    "maxResponseTime": {
        "total": "60001",
        "ok": "59988",
        "ko": "60001"
    },
    "meanResponseTime": {
        "total": "1501",
        "ok": "849",
        "ko": "12184"
    },
    "standardDeviation": {
        "total": "4724",
        "ok": "3544",
        "ko": "7834"
    },
    "percentiles1": {
        "total": "223",
        "ok": "3",
        "ko": "10000"
    },
    "percentiles2": {
        "total": "503",
        "ok": "500",
        "ko": "10001"
    },
    "percentiles3": {
        "total": "10000",
        "ok": "2642",
        "ko": "22478"
    },
    "percentiles4": {
        "total": "24244",
        "ok": "17315",
        "ko": "60000"
    },
    "group1": {
    "name": "t < 800 ms",
    "count": 25690,
    "percentage": 85
},
    "group2": {
    "name": "800 ms < t < 1200 ms",
    "count": 0,
    "percentage": 0
},
    "group3": {
    "name": "t > 1200 ms",
    "count": 2784,
    "percentage": 9
},
    "group4": {
    "name": "failed",
    "count": 1736,
    "percentage": 6
},
    "meanNumberOfRequestsPerSecond": {
        "total": "154.923",
        "ok": "146.021",
        "ko": "8.903"
    }
}
    }
}

}

function fillStats(stat){
    $("#numberOfRequests").append(stat.numberOfRequests.total);
    $("#numberOfRequestsOK").append(stat.numberOfRequests.ok);
    $("#numberOfRequestsKO").append(stat.numberOfRequests.ko);

    $("#minResponseTime").append(stat.minResponseTime.total);
    $("#minResponseTimeOK").append(stat.minResponseTime.ok);
    $("#minResponseTimeKO").append(stat.minResponseTime.ko);

    $("#maxResponseTime").append(stat.maxResponseTime.total);
    $("#maxResponseTimeOK").append(stat.maxResponseTime.ok);
    $("#maxResponseTimeKO").append(stat.maxResponseTime.ko);

    $("#meanResponseTime").append(stat.meanResponseTime.total);
    $("#meanResponseTimeOK").append(stat.meanResponseTime.ok);
    $("#meanResponseTimeKO").append(stat.meanResponseTime.ko);

    $("#standardDeviation").append(stat.standardDeviation.total);
    $("#standardDeviationOK").append(stat.standardDeviation.ok);
    $("#standardDeviationKO").append(stat.standardDeviation.ko);

    $("#percentiles1").append(stat.percentiles1.total);
    $("#percentiles1OK").append(stat.percentiles1.ok);
    $("#percentiles1KO").append(stat.percentiles1.ko);

    $("#percentiles2").append(stat.percentiles2.total);
    $("#percentiles2OK").append(stat.percentiles2.ok);
    $("#percentiles2KO").append(stat.percentiles2.ko);

    $("#percentiles3").append(stat.percentiles3.total);
    $("#percentiles3OK").append(stat.percentiles3.ok);
    $("#percentiles3KO").append(stat.percentiles3.ko);

    $("#percentiles4").append(stat.percentiles4.total);
    $("#percentiles4OK").append(stat.percentiles4.ok);
    $("#percentiles4KO").append(stat.percentiles4.ko);

    $("#meanNumberOfRequestsPerSecond").append(stat.meanNumberOfRequestsPerSecond.total);
    $("#meanNumberOfRequestsPerSecondOK").append(stat.meanNumberOfRequestsPerSecond.ok);
    $("#meanNumberOfRequestsPerSecondKO").append(stat.meanNumberOfRequestsPerSecond.ko);
}
