# Engineering evidence

## Asynchronous cover streaming

The Auralis `CoverProvider` now uses Android's `ContentProvider.openPipeHelper`
to return a data pipe and stream local cover bytes from the helper's background
writer callback. This replaces direct cover retrieval from `openFile()`.

The relevant Android API describes `openPipeHelper` as a helper for creating a
data pipe and background thread when streaming generated data to a client.

- Android Developers: [ContentProvider.openPipeHelper](https://developer.android.com/reference/android/content/ContentProvider#openPipeHelper(android.net.Uri,%20java.lang.String,%20android.os.Bundle,%20T,%20android.content.ContentProvider.PipeDataWriter%3CT%3E))
