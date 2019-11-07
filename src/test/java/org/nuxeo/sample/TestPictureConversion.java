package org.nuxeo.sample;

import static org.junit.Assert.*;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import javax.imageio.ImageIO;
import javax.inject.Inject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.nuxeo.common.utils.FileUtils;

import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.PictureConversion;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.ecm.core.event.EventService;



/**
 * Hi
 * based on TestImagingConvertPlugin
 */
@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.sample.nxp-28274-core")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.picture.convert")
@Deploy("org.nuxeo.ecm.platform.tag")

public class TestPictureConversion {

    @Inject
    protected CoreSession session;

    @Inject
    protected ConversionService conversionService;

    @Inject
    protected ImagingService imagingService;

    @Inject
    protected EventService eventService;

    @Inject
    protected TransactionalFeature txFeature;

    public static File getFileFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assert file.length() > 0;
        return file;
    }

    @Test
    public void iShouldHaveCmdLineResizeConverterRegistered() {
        ConverterCheckResult check = Framework.getService(ConversionService.class).isConverterAvailable("cmdLineResize");
        assertTrue(check.isAvailable());
    }

    @Test
    @Ignore
    public void iCanRunConverter() {
        String converter = "cmdLineResize";

        Map<String, Serializable> options = new HashMap<>();
        options.put("newResolution","300x300");

        String filename = "hedonist-pomegranate-careless-fables-pool.png";
        String path = "data/" + filename;
        try {
            Blob blob = Blobs.createBlob(getFileFromPath(path));
            blob.setFilename(filename);
            MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
            String mimeType = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault(filename, blob, null);
            blob.setMimeType(mimeType);
            BlobHolder bh = new SimpleBlobHolder(blob);
            BlobHolder result = conversionService.convert(converter, bh, options);
            assertNotNull(result);

            ImageInfo imageInfo = imagingService.getImageInfo(result.getBlob());

            assertEquals(imageInfo.getWidth(),300);


        }
        catch (IOException e) {
            // BOO
        }
    }
    
    @Test
    public void iCanGetViews() {
        DocumentModel picture = session.createDocumentModel("/", "picture", "Picture");
        String filename = "hedonist-pomegranate-careless-fables-pool.png";
        String path = "data/" + filename;
        try {
	        Blob blob = Blobs.createBlob(getFileFromPath(path));
	        blob.setFilename(filename);
	        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
	        String mimeType = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault(filename, blob, null);
	        blob.setMimeType(mimeType);
	        picture.setPropertyValue("file:content", (Serializable) blob);
	        picture = session.createDocument(picture);

            txFeature.nextTransaction();

            // Wait for the end of all the async works
            eventService.waitForAsyncCompletion();
	        
	        MultiviewPicture multiviewPicture = picture.getAdapter(MultiviewPicture.class);

            // based on TestPictureConversions and TestImagingAdapter
	        PictureView c = multiviewPicture.getView("cmdLineResize");
	        assertNotNull(c);
            // size here will be that of the original -- ImagingComponent doesn't know what the converter is going to use
	        assertEquals(4000, c.getHeight());
	        assertEquals(4000, c.getWidth());

	        ImageInfo imageInfo = c.getImageInfo();
	        assertNotNull(imageInfo);
	        assertEquals(3000, imageInfo.getWidth());
	        assertEquals(3000, imageInfo.getHeight());

//	        check the image to see if it has the expected dimensions and formatting
	        BufferedImage image = ImageIO.read(c.getBlob().getStream());
	        assertNotNull("Resized image is null", image);
	        assertEquals("Resized image height", 3000, image.getHeight());

	        c = multiviewPicture.getView("cmdLineResizeWithMaxSize");
	        assertNotNull(c);
            // Size here will be whatever maxSize is set to
	        assertEquals(1000, c.getHeight());
	        assertEquals(1000, c.getWidth());

	        imageInfo = c.getImageInfo();
	        assertNotNull(imageInfo);
	        assertEquals(3000, imageInfo.getWidth());
	        assertEquals(3000, imageInfo.getHeight());

	        image = ImageIO.read(c.getBlob().getStream());
	        assertNotNull("Resized image is null", image);
	        assertEquals("Resized image height", 3000, image.getHeight());
	        
        } catch (IOException e) {
        	
        }
    }
    
}
