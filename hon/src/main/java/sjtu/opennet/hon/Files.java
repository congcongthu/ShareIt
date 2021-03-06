package sjtu.opennet.hon;

import mobile.Mobile_;
import mobile.SearchHandle;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.Model.Block;
import sjtu.opennet.textilepb.QueryOuterClass;
import sjtu.opennet.textilepb.View.FilesList;

/**
 * Provides access to Textile files related APIs
 */
public class Files extends NodeDependent {

    Files(final Mobile_ node) {
        super(node);
    }

    /**
     * Add raw data to to a Textile thread
     * @param base64 Raw data as a base64 string
     * @param threadId The thread id the data will be added to
     * @param caption A caption for the input
     * @param handler An object that will get called with the resulting block
     */
    public void addData(final String base64, final String threadId, final String caption, final Handlers.BlockHandler handler) {
        node.addData(base64, threadId, caption, (data, e) -> {
            if (e != null) {
                handler.onError(e);
                return;
            }
            try {
                handler.onComplete(Block.parseFrom(data));
            } catch (final Exception exception) {
                handler.onError(exception);
            }
        });
    }

    /**
     * Add file(s) to to a Textile thread
     * @param files A comma-separated list of file paths
     * @param threadId The thread id the data will be added to
     * @param caption A caption for the input
     * @param handler An object that will get called with the resulting block
     */
    public void addFiles(final String files, final String threadId, final String caption, final Handlers.BlockHandler handler) {
        node.addFiles(files, threadId, caption, (data, e) -> {
            if (e != null) {
                handler.onError(e);
                return;
            }
            try {
                handler.onComplete(Block.parseFrom(data));
            } catch (final Exception exception) {
                handler.onError(exception);
            }
        });
    }

    public void addPicture(final String files, final String threadId, final String caption, final Handlers.BlockHandler handler) {
        node.addPicture(files, threadId, caption, (data, e) -> {
            if (e != null) {
                handler.onError(e);
                return;
            }
            try {
                handler.onComplete(Block.parseFrom(data));
            } catch (final Exception exception) {
                handler.onError(exception);
            }
        });
    }

    public void addSimpleFile(final String path, final String threadId, final Handlers.BlockHandler handler) {
        node.addSimpleFile(path, threadId, (data, e) -> {
            if (e != null) {
                handler.onError(e);
                return;
            }
            try {
                handler.onComplete(Block.parseFrom(data));
            } catch (final Exception exception) {
                handler.onError(exception);
            }
        });
    }

    public void addSimplePicture(final String path, final String threadId, final Handlers.BlockHandler handler) {
        node.addSimplePicture(path, threadId, (data, e) -> {
            if (e != null) {
                handler.onError(e);
                return;
            }
            try {
                handler.onComplete(Block.parseFrom(data));
            } catch (final Exception exception) {
                handler.onError(exception);
            }
        });
    }

    /**
     * Share files to a Textile thread
     * @param hash The hash of the files graph to share
     * @param threadId The thread id the data will be added to
     * @param caption A caption for the shared input
     * @param handler An object that will get called with the resulting block
     */
    public void shareFiles(final String hash, final String threadId, final String caption, final Handlers.BlockHandler handler) {
        node.shareFiles(hash, threadId, caption, (data, e) -> {
            if (e != null) {
                handler.onError(e);
                return;
            }
            try {
                handler.onComplete(Block.parseFrom(data));
            } catch (final Exception exception) {
                handler.onError(exception);
            }
        });
    }

    /**
     * Get a list of files data from a thread
     * @param threadId The thread to query
     * @param offset The offset to beging querying from
     * @param limit The max number of results to return
     * @return An object containing a list of files data
     * @throws Exception The exception that occurred
     */
    public FilesList list(final String threadId, final String offset, final long limit) throws Exception {
        final byte[] bytes = node.files(threadId, offset, limit);
        return FilesList.parseFrom(bytes != null ? bytes : new byte[0]);
    }

    /**
     * Get raw data for a file hash
     * @param hash The hash to return data for
     * @param handler An object that will get called with the resulting data and media type
     */
    public void content(final String hash, final Handlers.DataHandler handler) {
        node.fileContent(hash, (data, media, e) -> {
            if (e != null) {
                handler.onError(e);
                return;
            }
            try {
                handler.onComplete(data, media);
            } catch (final Exception exception) {
                handler.onError(exception);
            }
        });
    }

    /**
     * Helper function to return the most appropriate image data for a minimun image width
     * @param path The IPFS path that includes image data for various image sizes
     * @param minWidth The width of the image the data will be used for
     * @param handler An object that will get called with the resulting data and media type
     */
    public void imageContentForMinWidth(final String path, final long minWidth, final Handlers.DataHandler handler) {
        node.imageFileContentForMinWidth(path, minWidth, (data, media, e) -> {
            if (e != null) {
                handler.onError(e);
                return;
            }
            try {
                handler.onComplete(data, media);
            } catch (final Exception exception) {
                handler.onError(exception);
            }
        });
    }

    public void addSyncFile(final Model.SyncFile sFile) throws Exception {
        node.addSyncFile(sFile.toByteArray());
    }

    public void publishSuynFile(final Model.SyncFile sFile) throws Exception {
        node.publishSyncFile(sFile.toByteArray());
    }
    public SearchHandle searchSyncFiles(final QueryOuterClass.SyncFileQuery query, final QueryOuterClass.QueryOptions options) throws Exception {
        //node.listSyncFile()
        return node.searchSyncFiles(query.toByteArray(), options.toByteArray());
    }

    public Model.SyncFileList listSyncFile(final String address, Model.SyncFile.Type sType) throws Exception{
        final byte[] bytes = node.listSyncFile(address, sType.getNumber());
        try {
            return Model.SyncFileList.parseFrom(bytes);
        }catch (NullPointerException e){
            return null;
        }
    }
}
