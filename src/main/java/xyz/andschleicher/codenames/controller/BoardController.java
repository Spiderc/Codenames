package xyz.andschleicher.codenames.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import xyz.andschleicher.codenames.GameUtils;
import xyz.andschleicher.codenames.HibernateUtil;
import xyz.andschleicher.codenames.bean.Board;
import xyz.andschleicher.codenames.bean.Player;
import xyz.andschleicher.codenames.bean.User;
import xyz.andschleicher.codenames.bean.WordList;
import xyz.andschleicher.codenames.bean.WordList.Word;

@RestController
@RequestMapping(value="/board")
public class BoardController {

	@SuppressWarnings("unchecked")
	@RequestMapping(value="/get", method=RequestMethod.GET)
	public List<Board> getBoard(HttpServletRequest request){
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		
		Query query = session.createQuery("from Board");
		
		List<Board> list = query.list();
		
		if(request != null) {
			String role = GameUtils.playerFromJWT(request).getRole();
			if(!"RM".equals(role) && !"BM".equals(role)) {
				for(Board board:list) {
					if(!board.isRevealed()) {
						board.setType(null);
					}
				}
			}
		}
		
		return list;
	}
	
	public void addBoard(Board board){
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		
		session.save(board);
		session.getTransaction().commit();
		session.close();
	}
	
	@SuppressWarnings("unchecked")
	public void updateBoard(Board board){
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		
		Query query = session.createQuery("from Board where wordLoc=:wordLoc");
		query.setParameter("wordLoc", board.getWordLoc());
		
		session.getTransaction().commit();
		List<Board> list = query.list();
		
		session.close();
		if(!list.isEmpty()) {
			session = HibernateUtil.getSessionFactory().openSession();
			session.beginTransaction();
			
			populateNullFields(board, list.get(0));
			
			session.update(board);
			session.getTransaction().commit();
			session.close();
		}
	}
	
	@RequestMapping(value="/reveal", method=RequestMethod.POST)
	public void reveal(@RequestBody Board board, HttpServletRequest request){
		String callingTeam = GameUtils.playerFromJWT(request).getRole();
		
		if((callingTeam.equals("R") && GameUtils.currentTurn.equals("Red")) || (callingTeam.equals("B") && GameUtils.currentTurn.equals("Blue"))) {
			PlayerController playerController = new PlayerController();
			if(playerController.reveal(callingTeam, board.getWordLoc())) {
				board.setRevealed(true);
				updateBoard(board);
			}
			
			String winningTeam = getWinningTeam();
			if(winningTeam != null) {
				UserController userController = new UserController();
				
				List<Player> players = playerController.getPlayers();
				List<String> teams = new ArrayList<String>();
				
				for(Player player:players) {
					User user = userController.getUser(player.getUsername());
					teams.add(user.getStatsCol(), player.getRole());
				}
				
				String results = winningTeam + "\t" + String.join("\t", teams);
				System.out.println(results);
			}
		}
	}
	
	public void clearBoard() {
		for(Board board:getBoard(null)) {
			Session session = HibernateUtil.getSessionFactory().openSession();
			session.beginTransaction();
			
			session.delete(board);
			session.getTransaction().commit();
			session.close();
		}
	}
	
	public void startGame(String wordListString){		
		clearBoard();
		
		PlayerController playerController = new PlayerController();
		List<String> colorArray = new ArrayList<String>();
		
		colorArray.add("A");
		for(int i=0;i<8;i++) {
			colorArray.add("R");
			colorArray.add("B");
			if(i != 7) {
				colorArray.add("G");
			}
		}
		
		if(GameUtils.randomNumber(0,1) == 0) {
			GameUtils.startingTeam = "Red";
			GameUtils.currentTurn = "Red";
			colorArray.add("R");
		} else {
			GameUtils.startingTeam = "Blue";
			GameUtils.currentTurn = "Blue";
			colorArray.add("B");
		}
		playerController.assignTeams();
		
		String[] wordLists = wordListString.split(",");
		List<Word> possibleWords = new ArrayList<Word>();
		
		try {
			FileInputStream fis = new FileInputStream(new File("src/main/resources/words.json"));
			Reader reader = new InputStreamReader(fis);
			Gson gson = new Gson();
			WordList[] wordList = gson.fromJson(reader, WordList[].class);
			
			for(int i=0;i<wordList.length;i++) {
				for(String list:wordLists) {
					if(wordList[i].getName().equals(list)) {
						possibleWords.addAll(wordList[i].getWords());
						break;
					}
				}
			}
			
			System.out.println(possibleWords.size());
		} catch (FileNotFoundException e) {
			
		}
		
		if(possibleWords.size() >= 25) {
			for(int i=0;i<25;i++) {
				Board board = new Board();
				board.setWordLoc(i);
				board.setWord(possibleWords.remove(GameUtils.randomNumber(0, possibleWords.size() - 1)).getWord());
				board.setType(colorArray.remove(GameUtils.randomNumber(0, colorArray.size() - 1)));
				
				addBoard(board);
			}
		}
	}
	
	private String getWinningTeam() {
		String winningTeam = null;
		
		List<Board> boards = getBoard(null);
		
		int redRevealed = 0;
		int blueRevelaed = 0;
		for(Board board:boards) {
			if(board.isRevealed()) {
				if(board.getType().equals("R")) {
					redRevealed = redRevealed + 1;
				} else if(board.getType().equals("B")) {
					blueRevelaed = blueRevelaed + 1;
				} else if(board.getType().equals("A")) {
					if(GameUtils.currentTurn.equals("Red")) {
						winningTeam = "B";
					} else {
						winningTeam = "R";
					}
					break;
				}
			}
		}
		
		if((GameUtils.startingTeam.equals("Blue") && redRevealed == 8) || (GameUtils.startingTeam.equals("Red") && redRevealed == 9)) {
			winningTeam = "B";
		}
		
		if((GameUtils.startingTeam.equals("Red") && blueRevelaed == 8) || (GameUtils.startingTeam.equals("Blue") && blueRevelaed == 9)) {
			winningTeam = "B";
		}
		
		return winningTeam;
	}
	
	private void populateNullFields(Board target, Board source) {
		if(target.getWord() == null) {
			target.setWord(source.getWord());
		}
		if(target.getType() == null) {
			target.setType(source.getType());
		}
	}
}
