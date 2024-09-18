/*
 * Copyright 2018-present datagear.tech
 *
 * This file is part of DataGear.
 *
 * DataGear is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * DataGear is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with DataGear.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package org.datagear.web.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.datagear.analysis.DataSet;
import org.datagear.analysis.DataSetField;
import org.datagear.analysis.DataSetParam;
import org.datagear.analysis.DataSetQuery;
import org.datagear.analysis.ResolvedDataSetResult;
import org.datagear.analysis.support.AbstractResolvableResourceDataSet;
import org.datagear.analysis.support.DataSetFmkTemplateResolvers;
import org.datagear.analysis.support.ProfileDataSet;
import org.datagear.analysis.support.TemplateContext;
import org.datagear.analysis.support.TemplateResolvedDataSetResult;
import org.datagear.management.domain.AnalysisProjectAwareEntity;
import org.datagear.management.domain.CsvFileDataSetEntity;
import org.datagear.management.domain.CsvValueDataSetEntity;
import org.datagear.management.domain.DataSetEntity;
import org.datagear.management.domain.DirectoryFileDataSetEntity;
import org.datagear.management.domain.DtbsSource;
import org.datagear.management.domain.ExcelDataSetEntity;
import org.datagear.management.domain.FileSource;
import org.datagear.management.domain.HttpDataSetEntity;
import org.datagear.management.domain.JsonFileDataSetEntity;
import org.datagear.management.domain.JsonValueDataSetEntity;
import org.datagear.management.domain.SqlDataSetEntity;
import org.datagear.management.domain.User;
import org.datagear.management.service.AnalysisProjectService;
import org.datagear.management.service.DataSetEntityService;
import org.datagear.management.service.FileSourceService;
import org.datagear.management.util.DtbsSourceConnectionFactory;
import org.datagear.management.util.ManagementSupport;
import org.datagear.persistence.PagingData;
import org.datagear.util.FileUtil;
import org.datagear.util.IDUtil;
import org.datagear.util.IOUtil;
import org.datagear.util.StringUtil;
import org.datagear.web.util.AnalysisProjectAwareSupport;
import org.datagear.web.util.OperationMessage;
import org.datagear.web.util.WebDashboardQueryConverter;
import org.datagear.web.vo.APIDDataFilterPagingQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/**
 * 数据集控制器。
 * 
 * @author datagear@163.com
 *
 */
@Controller
@RequestMapping("/dataSet")
public class DataSetController extends AbstractDtbsSourceConnController
{
	@Autowired
	private DataSetEntityService dataSetEntityService;

	@Autowired
	private AnalysisProjectService analysisProjectService;

	@Autowired
	private File tempDirectory;

	@Autowired
	private WebDashboardQueryConverter webDashboardQueryConverter;

	@Autowired
	private FileSourceService fileSourceService;

	@Autowired
	private ManagementSupport managementSupport;

	@Autowired
	private AnalysisProjectAwareSupport analysisProjectAwareSupport;

	public DataSetController()
	{
		super();
	}

	public DataSetEntityService getDataSetEntityService()
	{
		return dataSetEntityService;
	}

	public void setDataSetEntityService(DataSetEntityService dataSetEntityService)
	{
		this.dataSetEntityService = dataSetEntityService;
	}

	public AnalysisProjectService getAnalysisProjectService()
	{
		return analysisProjectService;
	}

	public void setAnalysisProjectService(AnalysisProjectService analysisProjectService)
	{
		this.analysisProjectService = analysisProjectService;
	}

	public File getTempDirectory()
	{
		return tempDirectory;
	}

	public void setTempDirectory(File tempDirectory)
	{
		this.tempDirectory = tempDirectory;
	}

	public WebDashboardQueryConverter getWebDashboardQueryConverter()
	{
		return webDashboardQueryConverter;
	}

	public void setWebDashboardQueryConverter(WebDashboardQueryConverter webDashboardQueryConverter)
	{
		this.webDashboardQueryConverter = webDashboardQueryConverter;
	}

	public FileSourceService getFileSourceService()
	{
		return fileSourceService;
	}

	public void setFileSourceService(FileSourceService fileSourceService)
	{
		this.fileSourceService = fileSourceService;
	}

	public ManagementSupport getManagementSupport()
	{
		return managementSupport;
	}

	public void setManagementSupport(ManagementSupport managementSupport)
	{
		this.managementSupport = managementSupport;
	}

	public AnalysisProjectAwareSupport getAnalysisProjectAwareSupport()
	{
		return analysisProjectAwareSupport;
	}

	public void setAnalysisProjectAwareSupport(AnalysisProjectAwareSupport analysisProjectAwareSupport)
	{
		this.analysisProjectAwareSupport = analysisProjectAwareSupport;
	}

	@RequestMapping("/add/" + DataSetEntity.DATA_SET_TYPE_SQL)
	public String addForSql(HttpServletRequest request, HttpServletResponse response,
			Model model)
	{
		SqlDataSetEntity dataSet = createDftSqlDataSetEntity(request, response, model);
		setRequestAnalysisProject(request, response, dataSet);

		setFormModel(model, dataSet, REQUEST_ACTION_ADD, "saveAdd/" + DataSetEntity.DATA_SET_TYPE_SQL);

		return buildFormView(dataSet.getDataSetType());
	}

	protected SqlDataSetEntity createDftSqlDataSetEntity(HttpServletRequest request, HttpServletResponse response,
			Model model)
	{
		return new SqlDataSetEntity();
	}

	@RequestMapping(value = "/saveAdd/" + DataSetEntity.DATA_SET_TYPE_SQL, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveAddForSql(HttpServletRequest request, HttpServletResponse response,
			@RequestBody SqlDataSetEntity dataSet)
	{
		User user = getCurrentUser();

		inflateSaveAddBaseInfo(request, user, dataSet);
		trimAnalysisProjectAware(dataSet);
		trimSqlDataSetEntity(dataSet);

		ResponseEntity<OperationMessage> responseEntity = checkSaveSqlDataSetEntity(request, user, dataSet, null);

		if (responseEntity != null)
			return responseEntity;

		this.dataSetEntityService.add(user, dataSet);

		return optSuccessDataResponseEntity(request, dataSet);
	}

	@RequestMapping("/add/" + DataSetEntity.DATA_SET_TYPE_JsonValue)
	public String addForJsonValue(HttpServletRequest request, HttpServletResponse response,
			Model model)
	{
		JsonValueDataSetEntity dataSet = createDftJsonValueDataSetEntity(request, response, model);
		setRequestAnalysisProject(request, response, dataSet);

		setFormModel(model, dataSet, REQUEST_ACTION_ADD, "saveAdd/" + DataSetEntity.DATA_SET_TYPE_JsonValue);

		return buildFormView(dataSet.getDataSetType());
	}

	protected JsonValueDataSetEntity createDftJsonValueDataSetEntity(HttpServletRequest request,
			HttpServletResponse response, Model model)
	{
		return new JsonValueDataSetEntity();
	}

	@RequestMapping(value = "/saveAdd/" + DataSetEntity.DATA_SET_TYPE_JsonValue, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveAddForJsonValue(HttpServletRequest request,
			HttpServletResponse response, @RequestBody JsonValueDataSetEntity dataSet)
	{
		User user = getCurrentUser();

		inflateSaveAddBaseInfo(request, user, dataSet);
		trimAnalysisProjectAware(dataSet);

		ResponseEntity<OperationMessage> responseEntity = checkSaveJsonValueDataSetEntity(request, user, dataSet, null);

		if (responseEntity != null)
			return responseEntity;

		this.dataSetEntityService.add(user, dataSet);

		return optSuccessDataResponseEntity(request, dataSet);
	}

	@RequestMapping("/add/" + DataSetEntity.DATA_SET_TYPE_JsonFile)
	public String addForJsonFile(HttpServletRequest request, HttpServletResponse response,
			Model model)
	{
		JsonFileDataSetEntity dataSet = createDftJsonFileDataSetEntity(request, response, model);
		dataSet.setFileSourceType(DirectoryFileDataSetEntity.FILE_SOURCE_TYPE_UPLOAD);
		setRequestAnalysisProject(request, response, dataSet);

		addAvailableCharsetNames(model);
		setFormModel(model, dataSet, REQUEST_ACTION_ADD, "saveAdd/" + DataSetEntity.DATA_SET_TYPE_JsonFile);

		return buildFormView(dataSet.getDataSetType());
	}

	protected JsonFileDataSetEntity createDftJsonFileDataSetEntity(HttpServletRequest request,
			HttpServletResponse response, Model model)
	{
		return new JsonFileDataSetEntity();
	}

	@RequestMapping(value = "/saveAdd/" + DataSetEntity.DATA_SET_TYPE_JsonFile, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveAddForJsonFile(HttpServletRequest request, HttpServletResponse response,
			@RequestBody JsonFileDataSetEntity dataSet) throws Throwable
	{
		User user = getCurrentUser();

		inflateSaveAddBaseInfo(request, user, dataSet);
		trimAnalysisProjectAware(dataSet);
		trimDirectoryFileDataSetEntity(dataSet);

		ResponseEntity<OperationMessage> responseEntity = checkSaveJsonFileDataSetEntity(request, user, dataSet, null);

		if (responseEntity != null)
			return responseEntity;

		this.dataSetEntityService.add(user, dataSet);
		copyToDirectoryFileDataSetEntityDirectoryIf(dataSet, "");
		
		return optSuccessDataResponseEntity(request, dataSet);
	}

	@RequestMapping("/add/" + DataSetEntity.DATA_SET_TYPE_Excel)
	public String addForExcel(HttpServletRequest request, HttpServletResponse response,
			Model model)
	{
		ExcelDataSetEntity dataSet = createDftExcelDataSetEntity(request, response, model);
		dataSet.setFileSourceType(DirectoryFileDataSetEntity.FILE_SOURCE_TYPE_UPLOAD);
		dataSet.setNameRow(1);
		setRequestAnalysisProject(request, response, dataSet);

		setFormModel(model, dataSet, REQUEST_ACTION_ADD, "saveAdd/" + DataSetEntity.DATA_SET_TYPE_Excel);

		return buildFormView(dataSet.getDataSetType());
	}

	protected ExcelDataSetEntity createDftExcelDataSetEntity(HttpServletRequest request, HttpServletResponse response,
			Model model)
	{
		return new ExcelDataSetEntity();
	}

	@RequestMapping(value = "/saveAdd/" + DataSetEntity.DATA_SET_TYPE_Excel, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveAddForExcel(HttpServletRequest request, HttpServletResponse response,
			@RequestBody ExcelDataSetEntity dataSet) throws Throwable
	{
		User user = getCurrentUser();

		inflateSaveAddBaseInfo(request, user, dataSet);
		trimAnalysisProjectAware(dataSet);
		trimDirectoryFileDataSetEntity(dataSet);

		ResponseEntity<OperationMessage> responseEntity = checkSaveExcelDataSetEntity(request, user, dataSet, null);

		if (responseEntity != null)
			return responseEntity;

		this.dataSetEntityService.add(user, dataSet);
		copyToDirectoryFileDataSetEntityDirectoryIf(dataSet, "");

		return optSuccessDataResponseEntity(request, dataSet);
	}

	@RequestMapping("/add/" + DataSetEntity.DATA_SET_TYPE_CsvValue)
	public String addForCsvValue(HttpServletRequest request, HttpServletResponse response,
			Model model)
	{
		CsvValueDataSetEntity dataSet = createDftCsvValueDataSetEntity(request, response, model);
		dataSet.setNameRow(1);
		setRequestAnalysisProject(request, response, dataSet);

		setFormModel(model, dataSet, REQUEST_ACTION_ADD, "saveAdd/" + DataSetEntity.DATA_SET_TYPE_CsvValue);

		return buildFormView(dataSet.getDataSetType());
	}

	protected CsvValueDataSetEntity createDftCsvValueDataSetEntity(HttpServletRequest request,
			HttpServletResponse response, Model model)
	{
		return new CsvValueDataSetEntity();
	}

	@RequestMapping(value = "/saveAdd/" + DataSetEntity.DATA_SET_TYPE_CsvValue, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveAddForCsvValue(HttpServletRequest request, HttpServletResponse response,
			@RequestBody CsvValueDataSetEntity dataSet)
	{
		User user = getCurrentUser();

		inflateSaveAddBaseInfo(request, user, dataSet);
		trimAnalysisProjectAware(dataSet);

		ResponseEntity<OperationMessage> responseEntity = checkSaveCsvValueDataSetEntity(request, user, dataSet, null);

		if (responseEntity != null)
			return responseEntity;

		this.dataSetEntityService.add(user, dataSet);

		return optSuccessDataResponseEntity(request, dataSet);
	}

	@RequestMapping("/add/" + DataSetEntity.DATA_SET_TYPE_CsvFile)
	public String addForCsvFile(HttpServletRequest request, HttpServletResponse response,
			Model model)
	{
		CsvFileDataSetEntity dataSet = createDftCsvFileDataSetEntity(request, response, model);
		dataSet.setFileSourceType(DirectoryFileDataSetEntity.FILE_SOURCE_TYPE_UPLOAD);
		dataSet.setNameRow(1);
		setRequestAnalysisProject(request, response, dataSet);

		addAvailableCharsetNames(model);
		setFormModel(model, dataSet, REQUEST_ACTION_ADD, "saveAdd/" + DataSetEntity.DATA_SET_TYPE_CsvFile);

		return buildFormView(dataSet.getDataSetType());
	}

	protected CsvFileDataSetEntity createDftCsvFileDataSetEntity(HttpServletRequest request,
			HttpServletResponse response, Model model)
	{
		return new CsvFileDataSetEntity();
	}

	@RequestMapping(value = "/saveAdd/" + DataSetEntity.DATA_SET_TYPE_CsvFile, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveAddForCsvFile(HttpServletRequest request, HttpServletResponse response,
			@RequestBody CsvFileDataSetEntity dataSet) throws Throwable
	{
		User user = getCurrentUser();

		inflateSaveAddBaseInfo(request, user, dataSet);
		trimAnalysisProjectAware(dataSet);
		trimDirectoryFileDataSetEntity(dataSet);

		ResponseEntity<OperationMessage> responseEntity = checkSaveCsvFileDataSetEntity(request, user, dataSet, null);

		if (responseEntity != null)
			return responseEntity;

		this.dataSetEntityService.add(user, dataSet);
		copyToDirectoryFileDataSetEntityDirectoryIf(dataSet, "");
		
		return optSuccessDataResponseEntity(request, dataSet);
	}

	@RequestMapping("/add/" + DataSetEntity.DATA_SET_TYPE_Http)
	public String addForHttp(HttpServletRequest request, HttpServletResponse response,
			Model model)
	{
		HttpDataSetEntity dataSet = createDftHttpDataSetEntity(request, response, model);
		setRequestAnalysisProject(request, response, dataSet);
		dataSet.setEncodeUri(true);

		addAvailableCharsetNames(model);
		setFormModel(model, dataSet, REQUEST_ACTION_ADD, "saveAdd/" + DataSetEntity.DATA_SET_TYPE_Http);

		return buildFormView(dataSet.getDataSetType());
	}

	protected HttpDataSetEntity createDftHttpDataSetEntity(HttpServletRequest request, HttpServletResponse response,
			Model model)
	{
		return new HttpDataSetEntity();
	}

	@RequestMapping(value = "/saveAdd/" + DataSetEntity.DATA_SET_TYPE_Http, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveAddForHttp(HttpServletRequest request, HttpServletResponse response,
			@RequestBody HttpDataSetEntity dataSet)
	{
		User user = getCurrentUser();

		inflateSaveAddBaseInfo(request, user, dataSet);
		trimAnalysisProjectAware(dataSet);

		ResponseEntity<OperationMessage> responseEntity = checkSaveHttpDataSetEntity(request, user, dataSet, null);

		if (responseEntity != null)
			return responseEntity;

		this.dataSetEntityService.add(user, dataSet);
		
		return optSuccessDataResponseEntity(request, dataSet);
	}

	protected void inflateSaveAddBaseInfo(HttpServletRequest request, User user, DataSetEntity entity)
	{
		entity.setId(IDUtil.randomIdOnTime20());
		inflateCreateUserAndTime(entity, user);
	}

	@RequestMapping("/copy")
	public String copy(HttpServletRequest request, HttpServletResponse response, Model model,
			@RequestParam("id") String id) throws Throwable
	{
		User user = getCurrentUser();

		DataSetEntity dataSet = getByIdForView(this.dataSetEntityService, user, id);
		handlePageModelForCopy(request, model, user, dataSet);

		setFormModel(model, dataSet, REQUEST_ACTION_COPY, "saveAdd/" + dataSet.getDataSetType());

		return buildFormView(dataSet.getDataSetType());
	}

	protected void handlePageModelForCopy(HttpServletRequest request, Model model, User user, DataSetEntity dataSet)
			throws Throwable
	{
		if (dataSet instanceof SqlDataSetEntity)
		{
			SqlDataSetEntity dataSetEntity = (SqlDataSetEntity) dataSet;

			this.managementSupport.setRefNullIfNoPermission(user, dataSetEntity, (t) ->
			{
				DtbsSourceConnectionFactory connectionFactory = t.getConnectionFactory();
				return (connectionFactory == null ? null : connectionFactory.getDtbsSource());
			}, (t) ->
			{
				t.setConnectionFactory(null);
			}, getDtbsSourceService());
		}
		else if (dataSet instanceof DirectoryFileDataSetEntity)
		{
			DirectoryFileDataSetEntity dataSetEntity = (DirectoryFileDataSetEntity) dataSet;

			this.managementSupport.setRefNullIfNoPermission(user, dataSetEntity, (t) ->
			{
				return t.getFileSource();

			}, (t) ->
			{
				t.setFileSource(null);
				t.setDataSetResFileName(null);

			}, getFileSourceService());

			// 清空上传文件信息
			dataSetEntity.setDirectory(null);
			dataSetEntity.setFileName(null);
			dataSetEntity.setDisplayName(null);
		}

		this.analysisProjectAwareSupport.setNullIfNoPermission(user, dataSet, getAnalysisProjectService());
		dataSet.setId(null);

		convertForFormModel(dataSet);
		addAvailableCharsetNamesIfNeed(model, dataSet);
	}

	@RequestMapping("/edit")
	public String edit(HttpServletRequest request, HttpServletResponse response, Model model,
			@RequestParam("id") String id) throws Throwable
	{
		User user = getCurrentUser();

		DataSetEntity dataSet = getByIdForEdit(this.dataSetEntityService, user, id);
		handlePageModelForEdit(request, response, model, user, dataSet);

		setFormModel(model, dataSet, REQUEST_ACTION_EDIT, "saveEdit/" + dataSet.getDataSetType());

		return buildFormView(dataSet.getDataSetType());
	}

	protected void handlePageModelForEdit(HttpServletRequest request, HttpServletResponse response,
			Model model, User user, DataSetEntity entity) throws Throwable
	{
		convertForFormModel(entity);
		addAvailableCharsetNamesIfNeed(model, entity);
	}

	@RequestMapping(value = "/saveEdit/" + DataSetEntity.DATA_SET_TYPE_SQL, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveEditForSql(HttpServletRequest request, HttpServletResponse response,
			@RequestBody SqlDataSetEntity dataSet)
	{
		User user = getCurrentUser();

		trimAnalysisProjectAware(dataSet);
		trimSqlDataSetEntity(dataSet);

		ResponseEntity<OperationMessage> responseEntity = checkSaveSqlDataSetEntity(request, user, dataSet,
				(SqlDataSetEntity) getByIdForEdit(getDataSetEntityService(), user, dataSet.getId()));

		if (responseEntity != null)
			return responseEntity;

		this.dataSetEntityService.update(user, dataSet);
		
		return optSuccessDataResponseEntity(request, dataSet);
	}

	@RequestMapping(value = "/saveEdit/" + DataSetEntity.DATA_SET_TYPE_JsonValue, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveEditForJsonValue(HttpServletRequest request,
			HttpServletResponse response, @RequestBody JsonValueDataSetEntity dataSet)
	{
		User user = getCurrentUser();

		trimAnalysisProjectAware(dataSet);

		ResponseEntity<OperationMessage> responseEntity = checkSaveJsonValueDataSetEntity(request, user, dataSet,
				(JsonValueDataSetEntity) getByIdForEdit(getDataSetEntityService(), user, dataSet.getId()));

		if (responseEntity != null)
			return responseEntity;

		this.dataSetEntityService.update(user, dataSet);
		
		return optSuccessDataResponseEntity(request, dataSet);
	}

	@RequestMapping(value = "/saveEdit/" + DataSetEntity.DATA_SET_TYPE_JsonFile, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveEditForJsonFile(HttpServletRequest request,
			HttpServletResponse response, @RequestBody JsonFileDataSetEntity dataSet,
			@RequestParam("originalFileName") String originalFileName) throws Throwable
	{
		User user = getCurrentUser();

		trimAnalysisProjectAware(dataSet);
		trimDirectoryFileDataSetEntity(dataSet);

		ResponseEntity<OperationMessage> responseEntity = checkSaveJsonFileDataSetEntity(request, user, dataSet,
				(JsonFileDataSetEntity) getByIdForEdit(getDataSetEntityService(), user, dataSet.getId()));

		if (responseEntity != null)
			return responseEntity;

		this.dataSetEntityService.update(user, dataSet);
		copyToDirectoryFileDataSetEntityDirectoryIf(dataSet, originalFileName);
		
		return optSuccessDataResponseEntity(request, dataSet);
	}

	@RequestMapping(value = "/saveEdit/" + DataSetEntity.DATA_SET_TYPE_Excel, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveEditForExcel(HttpServletRequest request, HttpServletResponse response,
			@RequestBody ExcelDataSetEntity dataSet, @RequestParam("originalFileName") String originalFileName)
			throws Throwable
	{
		User user = getCurrentUser();

		trimAnalysisProjectAware(dataSet);
		trimDirectoryFileDataSetEntity(dataSet);

		ResponseEntity<OperationMessage> responseEntity = checkSaveExcelDataSetEntity(request, user, dataSet,
				(ExcelDataSetEntity) getByIdForEdit(getDataSetEntityService(), user, dataSet.getId()));

		if (responseEntity != null)
			return responseEntity;

		this.dataSetEntityService.update(user, dataSet);
		copyToDirectoryFileDataSetEntityDirectoryIf(dataSet, originalFileName);
		
		return optSuccessDataResponseEntity(request, dataSet);
	}

	@RequestMapping(value = "/saveEdit/" + DataSetEntity.DATA_SET_TYPE_CsvValue, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveEditForCsvValue(HttpServletRequest request,
			HttpServletResponse response, @RequestBody CsvValueDataSetEntity dataSet)
	{
		User user = getCurrentUser();

		trimAnalysisProjectAware(dataSet);

		ResponseEntity<OperationMessage> responseEntity = checkSaveCsvValueDataSetEntity(request, user, dataSet,
				(CsvValueDataSetEntity) getByIdForEdit(getDataSetEntityService(), user, dataSet.getId()));

		if (responseEntity != null)
			return responseEntity;

		this.dataSetEntityService.update(user, dataSet);
		
		return optSuccessDataResponseEntity(request, dataSet);
	}

	@RequestMapping(value = "/saveEdit/" + DataSetEntity.DATA_SET_TYPE_CsvFile, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveEditForCsvFile(HttpServletRequest request, HttpServletResponse response,
			@RequestBody CsvFileDataSetEntity dataSet, @RequestParam("originalFileName") String originalFileName)
			throws Throwable
	{
		User user = getCurrentUser();

		trimAnalysisProjectAware(dataSet);
		trimDirectoryFileDataSetEntity(dataSet);

		ResponseEntity<OperationMessage> responseEntity = checkSaveCsvFileDataSetEntity(request, user, dataSet,
				(CsvFileDataSetEntity) getByIdForEdit(getDataSetEntityService(), user, dataSet.getId()));

		if (responseEntity != null)
			return responseEntity;



		this.dataSetEntityService.update(user, dataSet);
		copyToDirectoryFileDataSetEntityDirectoryIf(dataSet, originalFileName);
		
		return optSuccessDataResponseEntity(request, dataSet);
	}

	@RequestMapping(value = "/saveEdit/" + DataSetEntity.DATA_SET_TYPE_Http, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> saveEditForHttp(HttpServletRequest request, HttpServletResponse response,
			@RequestBody HttpDataSetEntity dataSet)
	{
		User user = getCurrentUser();

		trimAnalysisProjectAware(dataSet);

		ResponseEntity<OperationMessage> responseEntity = checkSaveHttpDataSetEntity(request, user, dataSet,
				(HttpDataSetEntity) getByIdForEdit(getDataSetEntityService(), user, dataSet.getId()));

		if (responseEntity != null)
			return responseEntity;

		this.dataSetEntityService.update(user, dataSet);
		
		return optSuccessDataResponseEntity(request, dataSet);
	}

	@RequestMapping(value = "/uploadFile", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public Map<String, Object> uploadFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("file") MultipartFile multipartFile) throws Exception
	{
		File directory = getTempDataSetDirectory();
		String displayName = multipartFile.getOriginalFilename();
		File tmpFile = FileUtil.generateUniqueFile(directory, FileUtil.getExtension(displayName));
		String fileName = tmpFile.getName();

		writeMultipartFile(multipartFile, tmpFile);

		Map<String, Object> results = new HashMap<>();
		results.put("fileName", fileName);
		results.put("displayName", displayName);

		return results;
	}

	@RequestMapping(value = "/downloadFile")
	public void downloadFile(HttpServletRequest request, HttpServletResponse response, @RequestParam("id") String id)
			throws Exception
	{
		User user = getCurrentUser();

		DataSetEntity dataSet = getByIdForView(this.dataSetEntityService, user, id);

		if (!(dataSet instanceof DirectoryFileDataSetEntity))
			throw new IllegalInputException();

		DirectoryFileDataSetEntity dataSetEntity = (DirectoryFileDataSetEntity) dataSet;

		// 服务端文件允许参数化文件名因而无法再这里下载文件，即便如此，也可能保存着用户之前编辑的上传文件而允许下载，所以不应启用下面的逻辑
		// if
		// (!DirectoryFileDataSetEntity.FILE_SOURCE_TYPE_UPLOAD.equals(dataSetEntity.getFileSourceType()))
		// throw new IllegalInputException();

		if (isEmpty(dataSetEntity.getFileName()))
			throw new FileNotFoundException();

		File dataSetDirectory = getDataSetEntityService().getDataSetDirectory(dataSetEntity.getId());
		File entityFile = FileUtil.getFile(dataSetDirectory, dataSetEntity.getFileName());

		if (!entityFile.exists())
			throw new FileNotFoundException();

		String displayName = dataSetEntity.getDisplayName();

		response.setCharacterEncoding(IOUtil.CHARSET_UTF_8);
		setDownloadResponseHeader(request, response, displayName);
		OutputStream out = response.getOutputStream();

		IOUtil.write(entityFile, out);
	}

	@RequestMapping("/view")
	public String view(HttpServletRequest request, HttpServletResponse response, Model model,
			@RequestParam("id") String id) throws Throwable
	{
		User user = getCurrentUser();

		DataSetEntity dataSet = getByIdForView(this.dataSetEntityService, user, id);
		handlePageModelForView(request, response, model, user, dataSet);

		setFormModel(model, dataSet, REQUEST_ACTION_VIEW, SUBMIT_ACTION_NONE);

		return buildFormView(dataSet.getDataSetType());
	}

	protected void handlePageModelForView(HttpServletRequest request, HttpServletResponse response,
			Model model, User user, DataSetEntity entity) throws Throwable
	{
		convertForFormModel(entity);
		addAvailableCharsetNamesIfNeed(model, entity);
	}

	@RequestMapping(value = "/getProfileDataSetByIds", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public List<ProfileDataSet> getProfileDataSetByIds(HttpServletRequest request, HttpServletResponse response,
			Model model, @RequestParam("id") String[] ids)
	{
		List<ProfileDataSet> dataSets = new ArrayList<>();

		if (!isEmpty(ids))
		{
			User user = getCurrentUser();

			for (String id : ids)
			{
				ProfileDataSet dataSet = this.dataSetEntityService.getProfileDataSet(user, id);

				if (dataSet == null)
					throw new RecordNotFoundException();

				dataSets.add(dataSet);
			}
		}

		return dataSets;
	}

	@RequestMapping(value = "/delete", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResponseEntity<OperationMessage> delete(HttpServletRequest request, HttpServletResponse response,
			@RequestBody String[] ids) throws Throwable
	{
		User user = getCurrentUser();

		for (int i = 0; i < ids.length; i++)
		{
			String id = ids[i];
			this.dataSetEntityService.deleteById(user, id);
			handleForDelete(request, response, id);
		}

		return optSuccessResponseEntity(request);
	}

	protected void handleForDelete(HttpServletRequest request, HttpServletResponse response, String id) throws Throwable
	{
		File dataSetDirectory = getDataSetEntityService().getDataSetDirectory(id);
		FileUtil.deleteFile(dataSetDirectory);
	}

	@RequestMapping("/manage")
	public String manage(HttpServletRequest request, HttpServletResponse response,
			Model model)
	{
		model.addAttribute(KEY_REQUEST_ACTION, REQUEST_ACTION_MANAGE);
		setReadonlyAction(model);
		addAttributeForWriteJson(model, KEY_CURRENT_ANALYSIS_PROJECT,
				getRequestAnalysisProject(request, response, getAnalysisProjectService()));
		
		return "/dataSet/dataSet_table";
	}

	@RequestMapping(value = "/select")
	public String select(HttpServletRequest request, HttpServletResponse response, Model model)
	{
		setSelectAction(request, model);
		addAttributeForWriteJson(model, KEY_CURRENT_ANALYSIS_PROJECT,
				getRequestAnalysisProject(request, response, getAnalysisProjectService()));
		
		return "/dataSet/dataSet_table";
	}

	@RequestMapping(value = "/pagingQueryData", produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public PagingData<DataSetEntity> pagingQueryData(HttpServletRequest request, HttpServletResponse response,
			final Model springModel,
			@RequestBody(required = false) APIDDataFilterPagingQuery pagingQueryParam) throws Exception
	{
		User user = getCurrentUser();
		final APIDDataFilterPagingQuery pagingQuery = inflateAPIDDataFilterPagingQuery(request, pagingQueryParam);

		PagingData<DataSetEntity> pagingData = this.dataSetEntityService.pagingQuery(user, pagingQuery,
				pagingQuery.getDataFilter(), pagingQuery.getAnalysisProjectId());

		return pagingData;
	}

	@RequestMapping(value = "/preview/" + DataSetEntity.DATA_SET_TYPE_SQL, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public TemplateResolvedDataSetResult previewSql(HttpServletRequest request, HttpServletResponse response,
			Model springModel, @RequestBody SqlDataSetPreview preview) throws Throwable
	{
		User user = getCurrentUser();
		SqlDataSetEntity dataSet = preview.getDataSet();
		
		if(isEmpty(dataSet))
			throw new IllegalInputException();
		
		trimAnalysisProjectAware(dataSet);
		trimSqlDataSetEntity(dataSet);

		// 添加时
		if (StringUtil.isEmpty(dataSet.getId()))
		{
			checkSaveSqlDataSetEntity(request, user, dataSet, null);
		}
		// 查看时
		else if (preview.isView())
		{
			dataSet = (SqlDataSetEntity) getByIdForView(getDataSetEntityService(), user, dataSet.getId());
		}
		// 编辑时
		else
		{
			checkSaveSqlDataSetEntity(request, user, dataSet,
					(SqlDataSetEntity) getByIdForEdit(getDataSetEntityService(), user, dataSet.getId()));
		}
		
		DtbsSourceConnectionFactory connFactory = dataSet.getDtbsCnFty();
		DtbsSource dtbsSource = (connFactory == null ? null : connFactory.getDtbsSource());
		String dtbsSourceId = (dtbsSource == null ? null : dtbsSource.getId());
		
		if (StringUtil.isEmpty(dtbsSourceId))
			throw new IllegalInputException();

		dtbsSource = getDtbsSourceNotNull(dtbsSourceId);

		DtbsSourceConnectionFactory connectionFactory = new DtbsSourceConnectionFactory(getConnectionSource(),
				dtbsSource);
		dataSet.setConnectionFactory(connectionFactory);
		dataSet.setSqlValidator(this.dataSetEntityService.getSqlDataSetSqlValidator());

		DataSetQuery query = convertDataSetQuery(request, response, preview.getQuery(), dataSet);

		return dataSet.resolve(query);
	}

	@RequestMapping(value = "/resolveSql", produces = CONTENT_TYPE_HTML)
	@ResponseBody
	public String resolveSql(HttpServletRequest request, HttpServletResponse response,
			Model springModel, @RequestBody ResolveSqlParam resolveSqlParam) throws Throwable
	{
		return resolveSqlTemplate(request, response, resolveSqlParam.getSql(), resolveSqlParam.getParamValues(),
				resolveSqlParam.getDataSetParams());
	}

	@RequestMapping(value = "/preview/" + DataSetEntity.DATA_SET_TYPE_JsonValue, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public TemplateResolvedDataSetResult previewJsonValue(HttpServletRequest request, HttpServletResponse response,
			Model springModel, @RequestBody JsonValueDataSetPreview preview) throws Throwable
	{
		User user = getCurrentUser();
		JsonValueDataSetEntity dataSet = preview.getDataSet();

		if (isEmpty(dataSet))
			throw new IllegalInputException();

		trimAnalysisProjectAware(dataSet);

		// 添加时
		if (StringUtil.isEmpty(dataSet.getId()))
		{
			checkSaveJsonValueDataSetEntity(request, user, dataSet, null);
		}
		// 查看时
		else if (preview.isView())
		{
			dataSet = (JsonValueDataSetEntity) getByIdForView(getDataSetEntityService(), user, dataSet.getId());
		}
		// 编辑时
		else
		{
			checkSaveJsonValueDataSetEntity(request, user, dataSet,
					(JsonValueDataSetEntity) getByIdForEdit(getDataSetEntityService(), user, dataSet.getId()));
		}

		DataSetQuery query = convertDataSetQuery(request, response, preview.getQuery(), dataSet);

		return dataSet.resolve(query);
	}

	@RequestMapping(value = "/preview/" + DataSetEntity.DATA_SET_TYPE_JsonFile, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResolvedDataSetResult previewJsonFile(HttpServletRequest request, HttpServletResponse response,
			Model springModel, @RequestBody JsonFileDataSetEntityPreview preview,
			@RequestParam("originalFileName") String originalFileName)
			throws Throwable
	{
		final User user = getCurrentUser();
		JsonFileDataSetEntity dataSet = preview.getDataSet();

		if (isEmpty(dataSet))
			throw new IllegalInputException();

		trimAnalysisProjectAware(dataSet);
		trimDirectoryFileDataSetEntity(dataSet);

		// 添加时
		if (StringUtil.isEmpty(dataSet.getId()))
		{
			checkSaveJsonFileDataSetEntity(request, user, dataSet, null);
		}
		// 查看时
		else if (preview.isView())
		{
			dataSet = (JsonFileDataSetEntity) getByIdForView(getDataSetEntityService(), user, dataSet.getId());
		}
		// 编辑时
		else
		{
			checkSaveJsonFileDataSetEntity(request, user, dataSet,
					(JsonFileDataSetEntity) getByIdForEdit(getDataSetEntityService(), user, dataSet.getId()));
		}

		setDirectoryFileDataSetForPreview(user, dataSet, originalFileName);

		DataSetQuery query = convertDataSetQuery(request, response, preview.getQuery(), dataSet);

		return dataSet.resolve(query);
	}

	@RequestMapping(value = "/preview/" + DataSetEntity.DATA_SET_TYPE_Excel, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResolvedDataSetResult previewExcel(HttpServletRequest request, HttpServletResponse response,
			Model springModel, @RequestBody ExcelDataSetEntityPreview preview,
			@RequestParam("originalFileName") String originalFileName) throws Throwable
	{
		final User user = getCurrentUser();
		ExcelDataSetEntity dataSet = preview.getDataSet();

		if (isEmpty(dataSet))
			throw new IllegalInputException();

		trimAnalysisProjectAware(dataSet);
		trimDirectoryFileDataSetEntity(dataSet);

		// 添加时
		if (StringUtil.isEmpty(dataSet.getId()))
		{
			checkSaveExcelDataSetEntity(request, user, dataSet, null);
		}
		// 查看时
		else if (preview.isView())
		{
			dataSet = (ExcelDataSetEntity) getByIdForView(getDataSetEntityService(), user, dataSet.getId());
		}
		// 编辑时
		else
		{
			checkSaveExcelDataSetEntity(request, user, dataSet,
					(ExcelDataSetEntity) getByIdForEdit(getDataSetEntityService(), user, dataSet.getId()));
		}

		setDirectoryFileDataSetForPreview(user, dataSet, originalFileName);

		DataSetQuery query = convertDataSetQuery(request, response, preview.getQuery(), dataSet);

		return dataSet.resolve(query);
	}

	@RequestMapping(value = "/preview/" + DataSetEntity.DATA_SET_TYPE_CsvValue, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public TemplateResolvedDataSetResult previewCsvValue(HttpServletRequest request, HttpServletResponse response,
			Model springModel, @RequestBody CsvValueDataSetPreview preview) throws Throwable
	{
		final User user = getCurrentUser();
		CsvValueDataSetEntity dataSet = preview.getDataSet();

		if (isEmpty(dataSet))
			throw new IllegalInputException();

		trimAnalysisProjectAware(dataSet);

		// 添加时
		if (StringUtil.isEmpty(dataSet.getId()))
		{
			checkSaveCsvValueDataSetEntity(request, user, dataSet, null);
		}
		// 查看时
		else if (preview.isView())
		{
			dataSet = (CsvValueDataSetEntity) getByIdForView(getDataSetEntityService(), user, dataSet.getId());
		}
		// 编辑时
		else
		{
			checkSaveCsvValueDataSetEntity(request, user, dataSet,
					(CsvValueDataSetEntity) getByIdForEdit(getDataSetEntityService(), user, dataSet.getId()));
		}

		DataSetQuery query = convertDataSetQuery(request, response, preview.getQuery(), dataSet);

		return dataSet.resolve(query);
	}

	@RequestMapping(value = "/preview/" + DataSetEntity.DATA_SET_TYPE_CsvFile, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public ResolvedDataSetResult previewCsvFile(HttpServletRequest request, HttpServletResponse response,
			Model springModel, @RequestBody CsvFileDataSetEntityPreview preview,
			@RequestParam("originalFileName") String originalFileName) throws Throwable
	{
		final User user = getCurrentUser();
		CsvFileDataSetEntity dataSet = preview.getDataSet();

		if (isEmpty(dataSet))
			throw new IllegalInputException();

		trimAnalysisProjectAware(dataSet);
		trimDirectoryFileDataSetEntity(dataSet);

		// 添加时
		if (StringUtil.isEmpty(dataSet.getId()))
		{
			checkSaveCsvFileDataSetEntity(request, user, dataSet, null);
		}
		// 查看时
		else if (preview.isView())
		{
			dataSet = (CsvFileDataSetEntity) getByIdForView(getDataSetEntityService(), user, dataSet.getId());
		}
		// 编辑时
		else
		{
			checkSaveCsvFileDataSetEntity(request, user, dataSet,
					(CsvFileDataSetEntity) getByIdForEdit(getDataSetEntityService(), user, dataSet.getId()));
		}

		setDirectoryFileDataSetForPreview(user, dataSet, originalFileName);

		DataSetQuery query = convertDataSetQuery(request, response, preview.getQuery(), dataSet);

		return dataSet.resolve(query);
	}

	@RequestMapping(value = "/preview/" + DataSetEntity.DATA_SET_TYPE_Http, produces = CONTENT_TYPE_JSON)
	@ResponseBody
	public TemplateResolvedDataSetResult previewHttp(HttpServletRequest request, HttpServletResponse response,
			Model springModel, @RequestBody HttpDataSetEntityPreview preview) throws Throwable
	{
		final User user = getCurrentUser();
		HttpDataSetEntity dataSet = preview.getDataSet();

		if (isEmpty(dataSet))
			throw new IllegalInputException();

		trimAnalysisProjectAware(dataSet);

		// 添加时
		if (StringUtil.isEmpty(dataSet.getId()))
		{
			checkSaveHttpDataSetEntity(request, user, dataSet, null);
		}
		// 查看时
		else if (preview.isView())
		{
			dataSet = (HttpDataSetEntity) getByIdForView(getDataSetEntityService(), user, dataSet.getId());
		}
		// 编辑时
		else
		{
			checkSaveHttpDataSetEntity(request, user, dataSet,
					(HttpDataSetEntity) getByIdForEdit(getDataSetEntityService(), user, dataSet.getId()));
		}

		dataSet.setHttpClient(getDataSetEntityService().getHttpClient());

		DataSetQuery query = convertDataSetQuery(request, response, preview.getQuery(), dataSet);

		return dataSet.resolve(query);
	}
	
	protected void addAvailableCharsetNamesIfNeed(Model model, DataSetEntity entity)
	{
		String type = entity.getDataSetType();

		if (DataSetEntity.DATA_SET_TYPE_JsonFile.equals(type) || DataSetEntity.DATA_SET_TYPE_CsvFile.equals(type)
				|| DataSetEntity.DATA_SET_TYPE_Http.equals(type))
		{
			addAvailableCharsetNames(model);
		}
	}

	protected List<String> addAvailableCharsetNames(Model model)
	{
		List<String> names = getAvailableCharsetNames();
		addAttributeForWriteJson(model, "availableCharsetNames", names);

		return names;
	}

	protected void convertForFormModel(DataSetEntity entity)
	{
		if(entity instanceof SqlDataSetEntity)
		{
			SqlDataSetEntity sqlDataSetEntity = ((SqlDataSetEntity) entity);
			sqlDataSetEntity.clearDtbsSourceSensitiveInfo();
			sqlDataSetEntity.setSqlValidator(null);
			
			DtbsSourceConnectionFactory connectionFactory = sqlDataSetEntity.getConnectionFactory();
			if(connectionFactory != null)
			{
				connectionFactory = new DtbsSourceConnectionFactory(connectionFactory.getConnectionSource(), connectionFactory.getDtbsSource());
				connectionFactory.setConnectionSource(null);
				sqlDataSetEntity.setConnectionFactory(connectionFactory);
			}
		}
		
		if(entity instanceof DirectoryFileDataSetEntity)
		{
			DirectoryFileDataSetEntity dfDataSetEntity = ((DirectoryFileDataSetEntity) entity);
			dfDataSetEntity.setDirectory(null);
			FileSource fileSource = dfDataSetEntity.getFileSource();
			if (fileSource != null)
				fileSource.setDirectory(null);
		}
		
		if(entity instanceof HttpDataSetEntity)
			((HttpDataSetEntity) entity).setHttpClient(null);

		if (entity instanceof AbstractResolvableResourceDataSet<?>)
			((AbstractResolvableResourceDataSet<?>) entity).setCache(null);
	}

	protected void setRequestAnalysisProject(HttpServletRequest request, HttpServletResponse response,
			DataSetEntity entity)
	{
		setRequestAnalysisProjectIfValid(request, response, this.analysisProjectService, entity);
	}

	protected boolean copyToDirectoryFileDataSetEntityDirectoryIf(DirectoryFileDataSetEntity entity,
			String originalFileName) throws IOException
	{
		String fileName = entity.getFileName();

		if (isEmpty(entity.getId()))
			return false;

		if (StringUtil.isEquals(originalFileName, fileName))
			return false;

		File dataSetDirectory = getDataSetEntityService().getDataSetDirectory(entity.getId());
		FileUtil.clearDirectory(dataSetDirectory);

		File tmpFile = FileUtil.getFile(getTempDataSetDirectory(), fileName);
		File entityFile = FileUtil.getFile(dataSetDirectory, fileName);

		IOUtil.copy(tmpFile, entityFile);

		return true;
	}

	protected void setDirectoryFileDataSetForPreview(User user, DirectoryFileDataSetEntity dataSet, String originalFileName)
	{
		String fileName = dataSet.getFileName();

		if (!isEmpty(dataSet.getId()) && !isEmpty(originalFileName) && originalFileName.equals(fileName))
			dataSet.setDirectory(getDataSetEntityService().getDataSetDirectory(dataSet.getId()));
		else
			dataSet.setDirectory(getTempDataSetDirectory());
		
		FileSource dsr = dataSet.getFileSource();
		if(dsr != null && !isEmpty(dsr.getId()))
		{
			dsr = this.fileSourceService.getById(dsr.getId());
			dataSet.setFileSource(dsr);
		}
	}

	protected String buildFormView(String dataSetType)
	{
		return "/dataSet/dataSet_form_" + dataSetType;
	}

	protected File getTempDataSetDirectory()
	{
		return FileUtil.getDirectory(this.tempDirectory, "dataSet", true);
	}

	protected String resolveSqlTemplate(HttpServletRequest request, HttpServletResponse response, String source,
			Map<String, ?> paramValues, Collection<DataSetParam> dataSetParams)
	{
		DataSetQuery dataSetQuery = getWebDashboardQueryConverter().convert(paramValues, dataSetParams,
				getCurrentUser());
		return DataSetFmkTemplateResolvers.SQL.resolve(source, new TemplateContext(dataSetQuery.getParamValues()));
	}

	protected DataSetQuery convertDataSetQuery(HttpServletRequest request, HttpServletResponse response,
			DataSetQuery dataSetQuery, DataSet dataSet)
	{
		return getWebDashboardQueryConverter().convert(dataSetQuery, dataSet, getCurrentUser());
	}

	protected ResponseEntity<OperationMessage> checkSaveSqlDataSetEntity(HttpServletRequest request,
			User user, SqlDataSetEntity dataSet, SqlDataSetEntity persist)
	{
		if (isEmpty(dataSet.getConnectionFactory()))
			throw new IllegalInputException();

		if (isEmpty(dataSet.getConnectionFactory().getDtbsSource()))
			throw new IllegalInputException();

		if (isEmpty(dataSet.getConnectionFactory().getDtbsSource().getId()))
			throw new IllegalInputException();

		if (isBlank(dataSet.getSql()))
			throw new IllegalInputException();

		ResponseEntity<OperationMessage> responseEntity = checkSaveEntity(request, user, dataSet, persist);

		if (responseEntity != null)
			return responseEntity;

		this.managementSupport.checkSaveRefPermission(user, dataSet, persist, (t) ->
		{
			DtbsSourceConnectionFactory connFactory = dataSet.getDtbsCnFty();
			return (connFactory == null ? null : connFactory.getDtbsSource());

		}, (r) ->
		{
			return r.getTitle();

		}, getDtbsSourceService());

		return null;
	}

	protected ResponseEntity<OperationMessage> checkSaveJsonValueDataSetEntity(HttpServletRequest request,
			User user, JsonValueDataSetEntity dataSet, JsonValueDataSetEntity persist)
	{
		if (isEmpty(dataSet.getValue()))
			throw new IllegalInputException();

		return checkSaveEntity(request, user, dataSet, persist);
	}

	protected ResponseEntity<OperationMessage> checkSaveJsonFileDataSetEntity(HttpServletRequest request,
			User user, JsonFileDataSetEntity dataSet, JsonFileDataSetEntity persist)
	{
		if (isEmpty(dataSet.getFileName()) && isEmpty(dataSet.getDataSetResFileName()))
			throw new IllegalInputException();

		ResponseEntity<OperationMessage> re = checkSaveEntity(request, user, dataSet, persist);

		if (re != null)
			return re;

		checkFileSourceSaveRefPermission(request, user, dataSet, persist);

		return null;
	}

	protected ResponseEntity<OperationMessage> checkSaveExcelDataSetEntity(HttpServletRequest request,
			User user, ExcelDataSetEntity dataSet, ExcelDataSetEntity persist)
	{
		if (isEmpty(dataSet.getFileName()) && isEmpty(dataSet.getDataSetResFileName()))
			throw new IllegalInputException();

		ResponseEntity<OperationMessage> re = checkSaveEntity(request, user, dataSet, persist);

		if (re != null)
			return re;

		checkFileSourceSaveRefPermission(request, user, dataSet, persist);

		return null;
	}

	protected ResponseEntity<OperationMessage> checkSaveCsvValueDataSetEntity(HttpServletRequest request,
			User user, CsvValueDataSetEntity dataSet, CsvValueDataSetEntity persist)
	{
		if (isEmpty(dataSet.getValue()))
			throw new IllegalInputException();

		return checkSaveEntity(request, user, dataSet, persist);
	}

	protected ResponseEntity<OperationMessage> checkSaveCsvFileDataSetEntity(HttpServletRequest request,
			User user, CsvFileDataSetEntity dataSet, CsvFileDataSetEntity persist)
	{
		if (isEmpty(dataSet.getFileName()) && isEmpty(dataSet.getDataSetResFileName()))
			throw new IllegalInputException();

		ResponseEntity<OperationMessage> re = checkSaveEntity(request, user, dataSet, persist);

		if (re != null)
			return re;

		checkFileSourceSaveRefPermission(request, user, dataSet, persist);

		return null;
	}

	protected ResponseEntity<OperationMessage> checkSaveHttpDataSetEntity(HttpServletRequest request,
			User user, HttpDataSetEntity dataSet, HttpDataSetEntity persist)
	{
		if (isEmpty(dataSet.getUri()))
			throw new IllegalInputException();

		return checkSaveEntity(request, user, dataSet, persist);
	}

	protected ResponseEntity<OperationMessage> checkSaveEntity(HttpServletRequest request, User user,
			DataSetEntity dataSet, DataSetEntity persist)
	{
		if (isBlank(dataSet.getName()))
			throw new IllegalInputException();

		List<DataSetParam> params = dataSet.getParams();
		if (params != null)
		{
			Set<String> names = new HashSet<>();

			for (DataSetParam param : params)
			{
				String name = param.getName();

				if(isEmpty(name))
				{
					return optFailResponseEntity(request, HttpStatus.BAD_REQUEST, "paramNameRequired");
				}
				else
				{
					// 参数名限定为：不允许忽略大小写的重名。
					// 因为某些数据库是大小写不敏感的，如果不做此限定，存储时会因为违反存唯一约束而报错
					String upperCaseName = name.toUpperCase();

					if (names.contains(upperCaseName))
					{
						return optFailResponseEntity(request, HttpStatus.BAD_REQUEST,
								"paramNameMustBeUniqueIgnoreCase");
					}
					else
						names.add(upperCaseName);
				}
			}
		}

		List<DataSetField> fields = dataSet.getFields();
		if (fields != null)
		{
			Set<String> names = new HashSet<>();

			for (DataSetField field : fields)
			{
				String name = field.getName();

				if(isEmpty(name))
				{
					return optFailResponseEntity(request, HttpStatus.BAD_REQUEST, "fieldNameRequired");
				}
				else
				{
					// 字段名限定为：不允许忽略大小写的重名。
					// 因为某些数据库是大小写不敏感的，如果不做此限定，存储时会因为违反存唯一约束而报错
					String upperCaseName = name.toUpperCase();

					if (names.contains(upperCaseName))
					{
						return optFailResponseEntity(request, HttpStatus.BAD_REQUEST,
								"fieldNameMustBeUniqueIgnoreCase");
					}
					else
						names.add(upperCaseName);
				}
			}
		}

		this.analysisProjectAwareSupport.checkSavePermission(user, dataSet, persist, getAnalysisProjectService());

		return null;
	}

	protected void trimSqlDataSetEntity(SqlDataSetEntity entity)
	{
		this.managementSupport.trimRef(entity, (t) ->
		{
			DtbsSourceConnectionFactory connFactory = t.getDtbsCnFty();
			return (connFactory == null ? null : connFactory.getDtbsSource());

		}, (t) ->
		{
			t.setDtbsCnFty(null);
		});
	}

	protected void trimDirectoryFileDataSetEntity(DirectoryFileDataSetEntity entity)
	{
		this.managementSupport.trimRef(entity, (t) ->
		{
			return t.getFileSource();
		}, (t) ->
		{
			t.setFileSource(null);
		});
	}

	protected void trimAnalysisProjectAware(AnalysisProjectAwareEntity entity)
	{
		this.analysisProjectAwareSupport.trim(entity);
	}

	protected void checkFileSourceSaveRefPermission(HttpServletRequest request, User user,
			DirectoryFileDataSetEntity dataSet, DirectoryFileDataSetEntity persist)
	{
		this.managementSupport.checkSaveRefPermission(user, dataSet, persist, (t) ->
		{
			return t.getFileSource();

		}, (r) ->
		{
			return r.getName();

		}, getFileSourceService());
	}

	public static class AbstractDataSetPreview<T extends DataSet> implements ControllerForm
	{
		private static final long serialVersionUID = 1L;

		private T dataSet;

		private DataSetQuery query;

		/** 是否查看操作 */
		private boolean view = false;

		public AbstractDataSetPreview()
		{
			super();
		}

		public T getDataSet()
		{
			return dataSet;
		}

		public void setDataSet(T dataSet)
		{
			this.dataSet = dataSet;
		}

		public DataSetQuery getQuery()
		{
			return query;
		}

		public void setQuery(DataSetQuery query)
		{
			this.query = query;
		}

		public boolean isView()
		{
			return view;
		}

		public void setView(boolean view)
		{
			this.view = view;
		}
	}

	public static class SqlDataSetPreview extends AbstractDataSetPreview<SqlDataSetEntity>
	{
		private static final long serialVersionUID = 1L;

		public SqlDataSetPreview()
		{
			super();
		}
	}

	public static class JsonValueDataSetPreview extends AbstractDataSetPreview<JsonValueDataSetEntity>
	{
		private static final long serialVersionUID = 1L;

		public JsonValueDataSetPreview()
		{
			super();
		}
	}

	public static class JsonFileDataSetEntityPreview extends AbstractDataSetPreview<JsonFileDataSetEntity>
	{
		private static final long serialVersionUID = 1L;

		public JsonFileDataSetEntityPreview()
		{
			super();
		}
	}

	public static class ExcelDataSetEntityPreview extends AbstractDataSetPreview<ExcelDataSetEntity>
	{
		private static final long serialVersionUID = 1L;

		public ExcelDataSetEntityPreview()
		{
			super();
		}
	}

	public static class CsvValueDataSetPreview extends AbstractDataSetPreview<CsvValueDataSetEntity>
	{
		private static final long serialVersionUID = 1L;

		public CsvValueDataSetPreview()
		{
			super();
		}
	}

	public static class CsvFileDataSetEntityPreview extends AbstractDataSetPreview<CsvFileDataSetEntity>
	{
		private static final long serialVersionUID = 1L;
	}

	public static class HttpDataSetEntityPreview extends AbstractDataSetPreview<HttpDataSetEntity>
	{
		private static final long serialVersionUID = 1L;

		public HttpDataSetEntityPreview()
		{
			super();
		}
	}

	public static class ResolveSqlParam implements ControllerForm
	{
		private static final long serialVersionUID = 1L;

		private String sql;

		@SuppressWarnings("unchecked")
		private List<DataSetParam> dataSetParams = Collections.EMPTY_LIST;

		@SuppressWarnings("unchecked")
		private Map<String, Object> paramValues = Collections.EMPTY_MAP;

		public ResolveSqlParam()
		{
			super();
		}

		public ResolveSqlParam(String sql)
		{
			super();
			this.sql = sql;
		}

		public String getSql()
		{
			return sql;
		}

		public void setSql(String sql)
		{
			this.sql = sql;
		}

		public List<DataSetParam> getDataSetParams()
		{
			return dataSetParams;
		}

		public void setDataSetParams(List<DataSetParam> dataSetParams)
		{
			this.dataSetParams = dataSetParams;
		}

		public Map<String, Object> getParamValues()
		{
			return paramValues;
		}

		public void setParamValues(Map<String, Object> paramValues)
		{
			this.paramValues = paramValues;
		}
	}
}
