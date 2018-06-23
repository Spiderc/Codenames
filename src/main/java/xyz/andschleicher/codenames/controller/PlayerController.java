package xyz.andschleicher.codenames.controller;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import xyz.andschleicher.codenames.GameUtils;
import xyz.andschleicher.codenames.HibernateUtil;
import xyz.andschleicher.codenames.bean.Player;
import xyz.andschleicher.codenames.bean.User;

@RestController
@RequestMapping(value="/player")
public class PlayerController {

	@SuppressWarnings("unchecked")
	@RequestMapping(value="/get", method=RequestMethod.GET)
	public List<Player> getPlayers(){
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		
		Query query = session.createQuery("from Player");
		
		List<Player> list = query.list();
		return list;
	}
	
	@SuppressWarnings("unchecked")
	public Player getPlayer(String username){
		Player result = null;
		
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		
		Query query = session.createQuery("from Player where username=:username");
		query.setParameter("username", username);
		
		List<Player> list = query.list();
		if(!list.isEmpty()) {
			result = list.get(0);
		}
		return result;
	}
	
	@RequestMapping(value="/add", method=RequestMethod.POST)
	public void addPlayer(@RequestBody Player player){
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		
		session.save(player);
		session.getTransaction().commit();
	}
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value="/update", method=RequestMethod.PUT)
	public void updatePlayer(@RequestBody Player player){
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		
		Query query = session.createQuery("from Player where username=:username");
		query.setParameter("username", player.getUsername());
		
		session.getTransaction().commit();
		List<Player> list = query.list();
		
		session.close();
		if(!list.isEmpty()) {
			session = HibernateUtil.getSessionFactory().openSession();
			session.beginTransaction();
			
			populateNullFields(player, list.get(0));
			
			session.update(player);
			session.getTransaction().commit();
			session.close();
		}
	}
	
	public void assignTeams() {
		List<Player> players = getPlayers();
		
		Player redCM = players.remove(GameUtils.randomNumber(0, players.size() - 1));
		redCM.setRole("RM");
		updatePlayer(redCM);
		
		Player blueCM = players.remove(GameUtils.randomNumber(0, players.size() - 1));
		blueCM.setRole("BM");
		updatePlayer(blueCM);
		
		int redCount = (int) Math.ceil(players.size() / 2.0);
		int blueCount = (int) Math.floor(players.size() / 2.0);
		
		for(int i=0;i<redCount;i++) {
			Player redPlayer = players.remove(GameUtils.randomNumber(0, players.size() - 1));
			redPlayer.setRole("R");
			updatePlayer(redPlayer);
		}
		
		for(int i=0;i<blueCount;i++) {
			Player bluePlayer = players.remove(GameUtils.randomNumber(0, players.size() - 1));
			bluePlayer.setRole("B");
			updatePlayer(bluePlayer);
		}
	}
	
	@SuppressWarnings("unchecked")
	public boolean reveal(String team, int loc) {
		boolean reveal = false;
		
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		
		Query query = session.createQuery("from Player where role=:role and selected" + loc + "=false");
		query.setParameter("role", team);
		
		List<Player> list = query.list();
		if(list.isEmpty()) {
			reveal = true;
		}
		
		return reveal;
	}
	
	private void populateNullFields(Player target, Player source) {
		if(target.getRole() == null) {
			target.setRole(source.getRole());
		}
	}
}
