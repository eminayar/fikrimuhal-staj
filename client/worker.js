function delay(t, v) {
    return new Promise(function(resolve) {
        setTimeout(resolve.bind(null, v), t)
    });
}

function Stream(response){
    return new Promise( function(resolve) {
        response.then(function (rsp) {
            return rsp.body.getReader();
        }).then(function (reader) {
            var stream = new ReadableStream({
                start(controller) {
                    // The following function handles each data chunk
                    function push() {
                        // "done" is a Boolean and value a "Uint8Array"
                        reader.read().then(function (chunk) {
                            // Is there no more data to read?
                            console.log(chunk.done);
                            if (chunk.done) {
                                // Tell the browser that we have finished sending data
                                controller.close();
                                return;
                            }

                            // Get the data and send it to the browser via the controller
                            controller.enqueue(chunk.value);
                            delay(500).then(function(){push();});
                        });
                    }

                    push();
                }
            });
            resolve(new Response(stream, {headers: {"Content-Type": "text/html"}}));
        });
    });
}

self.addEventListener('fetch', function(event) {
    console.log(event);
    var request=event.request;
    var response = fetch(request);
    console.log(response);
    if (event.request.url.includes('/test.txt')) {
        event.respondWith( Stream(response) );
    }
});