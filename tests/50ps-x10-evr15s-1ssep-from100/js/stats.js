var stats = {
    type: "GROUP",
name: "Global Information",
path: "",
pathFormatted: "group_missing-name-b06d1",
stats: {
    "name": "Global Information",
    "numberOfRequests": {
        "total": "51675",
        "ok": "49392",
        "ko": "2283"
    },
    "minResponseTime": {
        "total": "1",
        "ok": "1",
        "ko": "8231"
    },
    "maxResponseTime": {
        "total": "60001",
        "ok": "58614",
        "ko": "60001"
    },
    "meanResponseTime": {
        "total": "1290",
        "ok": "827",
        "ko": "11305"
    },
    "standardDeviation": {
        "total": "4127",
        "ok": "3467",
        "ko": "4533"
    },
    "percentiles1": {
        "total": "2",
        "ok": "2",
        "ko": "10000"
    },
    "percentiles2": {
        "total": "503",
        "ok": "498",
        "ko": "10001"
    },
    "percentiles3": {
        "total": "10000",
        "ok": "3411",
        "ko": "20929"
    },
    "percentiles4": {
        "total": "20508",
        "ok": "16810",
        "ko": "31576"
    },
    "group1": {
    "name": "t < 800 ms",
    "count": 44083,
    "percentage": 85
},
    "group2": {
    "name": "800 ms < t < 1200 ms",
    "count": 0,
    "percentage": 0
},
    "group3": {
    "name": "t > 1200 ms",
    "count": 5309,
    "percentage": 10
},
    "group4": {
    "name": "failed",
    "count": 2283,
    "percentage": 4
},
    "meanNumberOfRequestsPerSecond": {
        "total": "270.55",
        "ok": "258.597",
        "ko": "11.953"
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
        "total": "51675",
        "ok": "49392",
        "ko": "2283"
    },
    "minResponseTime": {
        "total": "1",
        "ok": "1",
        "ko": "8231"
    },
    "maxResponseTime": {
        "total": "60001",
        "ok": "58614",
        "ko": "60001"
    },
    "meanResponseTime": {
        "total": "1290",
        "ok": "827",
        "ko": "11305"
    },
    "standardDeviation": {
        "total": "4127",
        "ok": "3467",
        "ko": "4533"
    },
    "percentiles1": {
        "total": "2",
        "ok": "2",
        "ko": "10000"
    },
    "percentiles2": {
        "total": "503",
        "ok": "498",
        "ko": "10001"
    },
    "percentiles3": {
        "total": "10000",
        "ok": "3411",
        "ko": "20929"
    },
    "percentiles4": {
        "total": "20549",
        "ok": "16813",
        "ko": "31576"
    },
    "group1": {
    "name": "t < 800 ms",
    "count": 44083,
    "percentage": 85
},
    "group2": {
    "name": "800 ms < t < 1200 ms",
    "count": 0,
    "percentage": 0
},
    "group3": {
    "name": "t > 1200 ms",
    "count": 5309,
    "percentage": 10
},
    "group4": {
    "name": "failed",
    "count": 2283,
    "percentage": 4
},
    "meanNumberOfRequestsPerSecond": {
        "total": "270.55",
        "ok": "258.597",
        "ko": "11.953"
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
