package com.peregrine.admin.servlets;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.peregrine.admin.data.PerAsset;
import com.peregrine.admin.replication.ImageMetadataSelector;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.models.factory.ModelFactory;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.peregrine.admin.servlets.ServletHelper.convertSuffixToParams;

@Component(
        service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=upload files servlet",
                Constants.SERVICE_VENDOR + "=headwire.com, Inc",
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=api/admin/uploadFiles",
                ServletResolverConstants.SLING_SERVLET_METHODS+"=POST"
        }
)
@SuppressWarnings("serial")
public class UploadFilesServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(UploadFilesServlet.class);

    @Reference
    ModelFactory modelFactory;

    private List<ImageMetadataSelector> imageMetadataSelectors = new ArrayList<>();

    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    )
    void addImageMetadataSelector(ImageMetadataSelector selector)    { imageMetadataSelectors.add(selector); }
    void removeImageMetadataSelector(ImageMetadataSelector selector) { imageMetadataSelectors.remove(selector); }

    @Override
    protected void doPost(SlingHttpServletRequest request,
                         SlingHttpServletResponse response) throws ServletException,
            IOException {

        Session session = request.getResourceResolver().adaptTo(Session.class);
        Map<String, String> params = convertSuffixToParams(request);
        String parentPath = params.get("path");
        try {
            Node node = session.getRootNode().getNode(parentPath.substring(1));
            log.debug(params.toString());
            for (Part part : request.getParts()) {
                log.debug("part type {}",part.getContentType());
                log.debug("part name {}",part.getName());
                Node newNode = node.addNode(part.getName(), "per:Asset");
                Node jcrContent = newNode.addNode("jcr:content", "per:AssetContent");
                Binary data = session.getValueFactory().createBinary(part.getInputStream());
                jcrContent.setProperty("jcr:data", data);
                jcrContent.setProperty("jcr:mimeType", part.getContentType());
                session.save();
                Resource assetResource = request.getResourceResolver().getResource(newNode.getPath());
                PerAsset perAsset = assetResource.adaptTo(PerAsset.class);
                try {
                    Metadata metadata = ImageMetadataReader.readMetadata(perAsset.getRenditionStream((Resource) null));
                    for(Directory directory : metadata.getDirectories()) {
                        String directoryName = directory.getName();
                        ImageMetadataSelector selector = null;
                        for(ImageMetadataSelector item: imageMetadataSelectors) {
                            String temp = item.acceptCategory(directoryName);
                            if(temp != null) {
                                selector = item;
                                directoryName = temp;
                            }
                        }
                        boolean asJson = selector != null && selector.asJsonProperty();
                        String json = "{";
                        for(Tag tag : directory.getTags()) {
                            String name = tag.getTagName();
                            String tagName = selector != null ? selector.acceptTag(name) : name;
                            if(tagName != null) {
                                log.debug("Add Tag, Category: '{}', Tag Name: '{}', Value: '{}'", new Object[]{directoryName, tagName, tag.getDescription()});
                                if(asJson) {
                                    json += "\"" + tagName + "\":\"" + tag.getDescription() + "\",";
                                } else {
                                    perAsset.addTag(directoryName, tagName, tag.getDescription());
                                }
                            }
                        }
                        if(asJson) {
                            if(json.length() > 1) {
                                json = json.substring(0, json.length() - 1);
                                json += "}";
                                perAsset.addTag(directoryName, "raw_tags", json);
                            }
                        }
                    }
                } catch(ImageProcessingException e) {
                    e.printStackTrace();
                }
            }
            log.debug("Upload Done successfully and saved");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (RepositoryException e) {
            log.debug("Upload Failed", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            e.printStackTrace(response.getWriter());
        }
    }

}

