package com.caa.controller;

import com.caa.dao.ExerciseDao;
import com.caa.dao.ProgramDao;
import com.caa.dao.ProgramExerciseItemDao;
import com.caa.model.Program;
import com.caa.model.ProgramExerciseItem;
import com.caa.modelview.ImageView;
import com.caa.modelview.ProgramExerciseItemView;
import com.caa.modelview.ProgramView;
import com.caa.report.ExportReport;
import com.caa.report.ProgramExercisesReportDTO;
import com.caa.services.ExerciseService;
import com.caa.services.ProgramService;
import com.caa.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
@CrossOrigin(origins = "*")
@RestController
@Transactional(isolation= Isolation.READ_COMMITTED)
public class ProgramController {

    private static Logger logger = LoggerFactory.getLogger(ProgramController.class);
	
    @Autowired
    private ProgramDao programDao;
    @Autowired
    private ExerciseDao exerciseDao;
    @Autowired
    private ProgramExerciseItemDao programExerciseItemDao;

    @Autowired
	ExerciseService exerciseService;

    @Autowired
	ProgramService programService;


    @RequestMapping(method = RequestMethod.GET, value = "/getProgram/{id:[\\d]+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Program getProgram(@PathVariable("id") long id) {

    	logger.info("getProgram entered: id= " + id);
    	
    	return programDao.findOne(id);
    }


    @RequestMapping(method = RequestMethod.GET, value = "/getProgramExerciseImage/{id:[\\d]+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ImageView getProgramexerciseImage(@PathVariable("id") long id) {

    	List<ProgramExerciseItemView> viewList = getProgramExerciseList(id);

		ExportReport exportReport = new ExportReport();
		List<ProgramExercisesReportDTO> reportDTOList = exerciseService.convertProgramExerciseToReportDTO(viewList);
		String imageBase64 = exportReport.getProgramExerciseAsImage(reportDTOList);

		ImageView imageView = new ImageView();
		imageView.setContent(imageBase64);
		return imageView;
		//return imageBase64;
    }

    private List<ProgramExerciseItemView> getProgramExerciseList(long id) {
		Program program = programDao.findOne(id);

		List<ProgramExerciseItem> exerciseItems = programExerciseItemDao.findByProgramId(program.getId());
		//TODO improve code
		List<ProgramExerciseItemView> viewList = new ArrayList<>();
		if (exerciseItems != null && exerciseItems.size() > 0) {
			exerciseItems.stream().forEach(programExerciseItem -> {
				ProgramExerciseItemView viewItem = new ProgramExerciseItemView(programExerciseItem);
				viewItem.setExerciseName(
						exerciseDao.getOne(programExerciseItem.getExerciseId()).getName());
				viewList.add(viewItem);
			});
		}
		return viewList;
	}

	//@RequestMapping(value="/getPrograms", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@RequestMapping(value="/getPrograms/{personId}")
	@ResponseBody
	public Iterable<ProgramView> getPrograms(@PathVariable("personId") long personId) {

		List<Program> list = programDao.findByPersonId(personId);
		List<ProgramView> result = new ArrayList<>();

		for(Program p : list) {
			ProgramView pv = new ProgramView(p);
			pv.setShamsiProgramDate(DateUtil.getShamsiDate(p.getProgramDate()));
			pv.setPersonName(p.getPerson().getFirstName() + " " + p.getPerson().getLastName()
			 + "(" + p.getPerson().getMobileNumber() + ")");

			List<ProgramExerciseItem> exerciseItems = programExerciseItemDao.findByProgramId(p.getId());
			//TODO improve code
			List<ProgramExerciseItemView> viewList = new ArrayList<>();
			if (exerciseItems != null && exerciseItems.size() > 0) {
				exerciseItems.stream().forEach(programExerciseItem -> {
					ProgramExerciseItemView viewItem = new ProgramExerciseItemView(programExerciseItem);
					viewItem.setExerciseName(
							exerciseDao.getOne(programExerciseItem.getExerciseId()).getName());
					viewList.add(viewItem);
				});
			}

			viewList.stream().forEach(exerciseItem -> {
				switch (exerciseItem.getSubExerciseId()) {
					case 1 : pv.getProgramExercise1Items().add(exerciseItem); break;
					case 2 : pv.getProgramExercise2Items().add(exerciseItem); break;
					case 3 : pv.getProgramExercise3Items().add(exerciseItem); break;
					case 4 : pv.getProgramExercise4Items().add(exerciseItem); break;
				}
			});
			result.add(pv);
		}
		return result;
	}

	@RequestMapping(value="/saveProgram", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Transactional
	public ProgramView saveProgram(@RequestBody ProgramView programView) {
    	
    	logger.info("saveProgram entered...");
    	Program p = getProgramFromView(programView);
		List<ProgramExerciseItemView> items1 = programView.getProgramExercise1Items();
		List<ProgramExerciseItemView> items2 = programView.getProgramExercise2Items();
		List<ProgramExerciseItemView> items3 = programView.getProgramExercise3Items();
		List<ProgramExerciseItemView> items4 = programView.getProgramExercise4Items();


		programDao.save(p);

		List<ProgramExerciseItem> allEntityList = new ArrayList<>();
		allEntityList.addAll(getSubExerciseItems(items1 , 1, p.getId()));
		allEntityList.addAll(getSubExerciseItems(items2 , 2, p.getId()));
		allEntityList.addAll(getSubExerciseItems(items3 , 3, p.getId()));
		allEntityList.addAll(getSubExerciseItems(items4 , 4, p.getId()));

		programExerciseItemDao.deleteByProgramId(p.getId());



		programExerciseItemDao.save(allEntityList);
		programView.setId(p.getId());
		return programView;
	}

	private List<ProgramExerciseItem> getSubExerciseItems(List<ProgramExerciseItemView> items,
														  int subExerciseIndex, Long pid) {
		if (items != null && items.size() > 0) {
			items.forEach(programExerciseItem -> {
				programExerciseItem.setProgramId(pid);
			});
		}

		List<ProgramExerciseItem> entityList = new ArrayList<>();
		items.stream().forEach(programExerciseItemView ->  {
			ProgramExerciseItem item = new ProgramExerciseItem();
			item.setId(programExerciseItemView.getId());
			item.setProgramId(programExerciseItemView.getProgramId());
			item.setExerciseId(programExerciseItemView.getExerciseId());
			item.setSubExerciseId(subExerciseIndex);
			item.setExerciseSet(programExerciseItemView.getExerciseSet());
			item.setExerciseRepeat(programExerciseItemView.getExerciseRepeat());
			item.setExerciseRepeatType(programExerciseItemView.getExerciseRepeatType());
			item.setDescription(programExerciseItemView.getDescription());
			entityList.add(item);
		});
		return entityList;
	}
	private Program getProgramFromView(ProgramView view) {
    	Program program = new Program();
    	program.setId(view.getId());
    	program.setDescription(view.getDescription());
    	program.setPerson(view.getPerson());
    	program.setPersonAbdomen(view.getPersonAbdomen());
    	program.setPersonAge(view.getPersonAge());
    	program.setPersonArm(view.getPersonArm());
    	program.setPersonChest(view.getPersonChest());
    	program.setPersonFatPercentage(view.getPersonFatPercentage());
    	program.setPersonFatWeight(view.getPersonFatWeight());
    	program.setPersonForeArm(view.getPersonForeArm());
    	program.setPersonId(view.getPerson().getId());
    	program.setPersonMuscleWeight(view.getPersonMuscleWeight());
    	program.setPersonScore(view.getPersonScore());
    	program.setPersonShin(view.getPersonShin());
    	program.setPersonButt(view.getPersonButt());
    	program.setPersonTall(view.getPersonTall());
    	program.setPersonThigh(view.getPersonThigh());
    	program.setPersonWaist(view.getPersonWaist());
    	program.setPersonWeight(view.getPersonWeight());
    	program.setProgramDate(DateUtil.getGregorianDate(view.getShamsiProgramDate()));
    	program.setProgramName(view.getProgramName());
    	return program;
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/deleteProgram/{id:[\\d]+}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional
	@ResponseBody
	public void deleteProgram(@PathVariable("id") long id) {
		logger.info("delete entered: id= " + id);
		 programExerciseItemDao.deleteByProgramId(id);
		 programDao.delete(id);
	}


	@RequestMapping(value = "/shareProgramImage/{id:[\\d]+}", method = RequestMethod.GET,
			produces = MediaType.IMAGE_JPEG_VALUE)
	public void getImage(HttpServletResponse response, @PathVariable("id") long id) throws IOException {
		List<ProgramExerciseItemView> viewList = getProgramExerciseList(id);
		List<ProgramExercisesReportDTO> reportDTOList = exerciseService.convertProgramExerciseToReportDTO(viewList);

		//		ExportReport exportReport = new ExportReport();
		//      String imageBase64 = exportReport.getProgramExerciseAsImage(reportDTOList);

		response.setContentType(MediaType.IMAGE_JPEG_VALUE);
		StreamUtils.copy(ExportReport.getProgramExerciseAsImageInBytes(reportDTOList), response.getOutputStream());
	}

	@RequestMapping(method = RequestMethod.GET, value = "/getPersonProgramsAllSizes/{id:[\\d]+}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String[][] getPersonProgramsAllSizes(@PathVariable("id") long id) {

		logger.info("getPersonAllSizes entered: id= " + id);

		return programService.getPersonAllSizes(id);
	}

}