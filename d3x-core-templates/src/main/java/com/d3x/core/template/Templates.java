/*
 * Copyright (C) 2018 D3X Systems - All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.d3x.core.template;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.d3x.core.http.server.HttpUtils;
import com.d3x.core.util.Consumers;
import com.d3x.core.util.Formatter;
import com.d3x.core.util.IO;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.Version;
import freemarker.template.WrappingTemplateModel;

/**
 * A class used to initialize and manage Freemarker related template entities with some customization
 *
 * @author  Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class Templates {

    private Configuration fmConfig;
    private Consumers<Map<String,Object>> consumers;

    /**
     * Constructor
     */
    public Templates() {
        this.consumers = new Consumers<>();
        this.fmConfig = new Configuration(Configuration.VERSION_2_3_27);
        this.fmConfig.setClassForTemplateLoading(Templates.class, "/");
        this.fmConfig.setDefaultEncoding("UTF-8");
        this.fmConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        this.fmConfig.setLogTemplateExceptions(true);
        this.fmConfig.setWrapUncheckedExceptions(true);
        this.fmConfig.setObjectWrapper(new CustomObjectWrapper(Configuration.VERSION_2_3_27));
    }


    /**
     * Attaches a template model consumer to be call each time a template is called
     * @param consumer  the consumer to inject default template model parameters
     */
    public void attach(Consumer<Map<String,Object>> consumer) {
        this.consumers.attach(consumer);
    }


    /**
     * Detaches a template model consumer from the interest list
     * @param consumer  the template model consumer to detach
     */
    public void detach(Consumer<Map<String,Object>> consumer) {
        this.consumers.detach(consumer);
    }


    /**
     * Returns a JAX-RS text/html response generated from a template
     * @param path          the path to template
     * @param req           the servlet request object
     * @param consumer      the model consumer
     * @return              the JAX-RS response
     */
    public Response html(String path, HttpServletRequest req, Consumer<Map<String,Object>> consumer) {
        return Response.ok(stream(path, req, consumer), MediaType.TEXT_HTML_TYPE).build();
    }


    /**
     * Returns the output of a template based on the path and model consumer
     * @param path      the path to the template
     * @param consumer  the model consumer
     * @return          the template result
     */
    public String output(String path, Consumer<Map<String,Object>> consumer) {
        Writer writer = null;
        try {
            final Map<String,Object> model = getModel(consumer);
            final ByteArrayOutputStream output = new ByteArrayOutputStream(1024 * 100);
            writer = new BufferedWriter(new OutputStreamWriter(output));
            final Template template = fmConfig.getTemplate(path);
            template.process(model, writer);
            return new String(output.toByteArray());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to write output for template at " + path, ex);
        } finally {
            IO.close(writer);
        }
    }


    /**
     * Returns a StreamingOutput object for a template at the path specified
     * @param path      the path to the template
     * @param req       the servlet request object
     * @param consumer  the model consumer
     * @return          the streaming output object
     */
    public StreamingOutput stream(String path, HttpServletRequest req, Consumer<Map<String,Object>> consumer) {
        try {
            final String baseUrl = HttpUtils.getBaseUrl(req);
            final Map<String,Object> model = getModel(m -> {
                m.put("baseUrl", baseUrl);
                if (consumer != null) {
                    consumer.accept(m);
                }
            });
            return (os) -> {
                BufferedWriter writer = null;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(os));
                    final Template template = fmConfig.getTemplate(path);
                    template.process(model, writer);
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to write output for template " + path, ex);
                } finally {
                    if (writer != null) {
                        writer.flush();
                        writer.close();
                    }
                }
            };
        } catch (Exception ex) {
            throw new RuntimeException("Failed to render template for " + path, ex);
        }
    }



    /**
     * Returns a newly initialized model for a template
     * @param consumer  the consumer to enrich model
     * @return          the newly initialized model
     */
    private Map<String,Object> getModel(Consumer<Map<String,Object>> consumer) {
        final Map<String,Object> model = new HashMap<>();
        model.put("statics", new BeansWrapper(Configuration.VERSION_2_3_27).getStaticModels());
        this.consumers.accept(model);
        if (consumer != null) {
            consumer.accept(model);
        }
        return model;
    }


    /**
     * A custom object wrapper that supports various custom data types
     */
    class CustomObjectWrapper extends DefaultObjectWrapper {

        /**
         * Constructor
         * @param version   the config version
         */
        CustomObjectWrapper(Version version) {
            super(version);
            this.setExposeFields(true);
        }


        @Override
        public TemplateModel wrap(Object value) throws TemplateModelException {
            if (value instanceof Date) {
                return new DateTemplateModel(this, (Date) value);
            } else {
                return super.wrap(value);
            }
        }

        @Override
        protected TemplateModel handleUnknownType(Object value) throws TemplateModelException {
            if (value instanceof Duration) {
                return new DurationTemplateModel(this, (Duration) value);
            } else if (value instanceof Date) {
                return new DateTemplateModel(this, (Date)value);
            } else {
                return super.handleUnknownType(value);
            }
        }
    }


    /**
     * A template model for a java.util.Date
     */
    class DateTemplateModel extends WrappingTemplateModel implements TemplateScalarModel {

        private Date date;

        /**
         * Constructor
         * @param objectWrapper     the default object wrapper
         * @param date              the date value
         */
        DateTemplateModel(ObjectWrapper objectWrapper, Date date) {
            super(objectWrapper);
            this.date = date;
        }

        @Override
        public String getAsString() {
            return date == null ? "-" : String.valueOf(date.getTime());
        }
    }


    /**
     * A simple template model for duration objects
     */
    class DurationTemplateModel extends WrappingTemplateModel implements TemplateScalarModel {

        private Duration duration;

        /**
         * Constructor
         * @param objectWrapper     the default object wrapper
         * @param duration          the duration object
         */
        DurationTemplateModel(ObjectWrapper objectWrapper, Duration duration) {
            super(objectWrapper);
            this.duration = duration;
        }


        @Override
        public String getAsString() {
            return duration == null ? "-" : Formatter.format(duration);
        }
    }

}
