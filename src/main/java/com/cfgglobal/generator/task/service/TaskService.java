package com.cfgglobal.generator.task.service;

import com.cfgglobal.generator.entity.CodeEntity;
import com.cfgglobal.generator.entity.CodeProject;
import com.cfgglobal.generator.entity.Task;
import com.google.common.collect.Maps;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Map;

@Slf4j
public class TaskService {

    public static List<String> processTask(CodeProject codeProject, Task task) {
        List<String> paths;
        List<CodeEntity> entities = codeProject.getEntities();
        Map<String, Object> scope = Maps.newHashMap();
        scope.put("project", codeProject);
        scope.put("entities", entities);
        codeProject.getUtilClasses().forEach(util->{


            BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
            TemplateHashModel staticModels = wrapper.getStaticModels();
            try {
                TemplateHashModel fileStatics =
                        (TemplateHashModel) staticModels.get(util.getName());
                scope.put(util.getSimpleName(), fileStatics);
            } catch (TemplateModelException e) {
                e.printStackTrace();
            }

        });

        codeProject.getTemplateEngine().putAll(scope);
        paths = task.run(codeProject, scope);
        return paths;
    }

    /**
     * @param root
     * @param task
     * @return 生成文件的路径
     */
    public static String processTemplate(CodeProject codeProject, Task task, Map<String, Object> root) {
    /*    List<TaskParam> params = task.getTaskParams();
        for (TaskParam param : params) {
            if (StrKit.isBlank(param.getStr("name"))) continue;
            String value = param.getStr("expression");
            Config.templateEngine().put(param.getStr("name"), Config.scriptHelper().exec(value, root));
        }*/
        String templateFilename = codeProject.getScriptHelper().exec(task.getTemplatePath(), root).toString();
        String folder = codeProject.getScriptHelper().exec(task.getFolder(), root).toString();
        folder = codeProject.getTargetPath() + File.separator + folder;
        File folderDir = new File(folder);
        if (!folderDir.exists()) {
            folderDir.mkdirs();
        }
        String filename = codeProject.getScriptHelper().exec(task.getFilename(), root).toString();
        String outputFilename = folder + File.separator + filename;
        log.debug(outputFilename);
        codeProject.getTemplateEngine().exec(templateFilename, outputFilename);
        return outputFilename;

    }


}
