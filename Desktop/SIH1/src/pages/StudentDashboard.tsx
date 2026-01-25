import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useAuth } from '@/hooks/useAuth';
import { supabase } from '@/integrations/supabase/client';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { 
  User, 
  MapPin, 
  BookOpen, 
  Star, 
  Briefcase, 
  TrendingUp,
  Bell,
  Settings,
  LogOut,
  Search,
  RefreshCw,
  MessageSquare
} from 'lucide-react';
import InternshipCard from '@/components/InternshipCard';
import { FeedbackDialog } from '@/components/FeedbackDialog';

interface UserProfile {
  id: string;
  name: string;
  qualification: string;
  location: string;
  sector_interest: string;
  category: string;
  experience_level: string;
  portfolio_url: string;
  linkedin_url: string;
  skills: string[];
}

interface Match {
  id: string;
  match_score: number;
  application_status: string;
  applied_at: string;
  internship_id: string;
  student_id: string;
  status: string;
  skill_match?: number;
  location_match?: boolean;
  rural_priority?: boolean;
  fresher_friendly?: boolean;
  match_reasons?: string[];
}

const StudentDashboard = () => {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [matches, setMatches] = useState<Match[]>([]);
  const [loading, setLoading] = useState(true);
  const [matchingLoading, setMatchingLoading] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [feedbackDialogOpen, setFeedbackDialogOpen] = useState(false);
  const [selectedMatchForFeedback, setSelectedMatchForFeedback] = useState<any>(null);

  useEffect(() => {
    if (!user) {
      navigate('/student/auth');
      return;
    }

    // Set mock data immediately
    setProfile({
      id: 'mock-student-id',
      name: 'Alex Johnson',
      qualification: 'Computer Science',
      location: 'New York',
      sector_interest: 'Tech',
      category: 'frontend',
      experience_level: 'Beginner',
      portfolio_url: 'https://alex-portfolio.com',
      linkedin_url: 'https://linkedin.com/in/alexjohnson',
      skills: ['React', 'Python', 'JavaScript']
    });

    setMatches([
      {
        id: 'mock-match-1',
        match_score: 95,
        application_status: 'matched',
        applied_at: new Date().toISOString(),
        internship_id: 'mock-internship-1',
        student_id: 'mock-student-id',
        status: 'active',
        skill_match: 90,
        location_match: true,
        rural_priority: false,
        fresher_friendly: true,
        match_reasons: ['Strong React skills', 'Good location match', 'Fresher-friendly position']
      },
      {
        id: 'mock-match-2',
        match_score: 88,
        application_status: 'applied',
        applied_at: new Date().toISOString(),
        internship_id: 'mock-internship-2',
        student_id: 'mock-student-id',
        status: 'pending',
        skill_match: 85,
        location_match: false,
        rural_priority: true,
        fresher_friendly: true,
        match_reasons: ['Python expertise', 'Rural priority initiative', 'Beginner-friendly']
      }
    ]);

    setNotifications([
      {
        id: 'mock-notif-1',
        title: 'New Match Found!',
        message: 'You have a 95% match with Tech Corp',
        type: 'success',
        created_at: new Date().toISOString()
      },
      {
        id: 'mock-notif-2',
        title: 'Application Update',
        message: 'Your application to StartupXYZ has been shortlisted',
        type: 'info',
        created_at: new Date().toISOString()
      }
    ]);

    setLoading(false);
  }, [user, navigate]);

  const findNewMatches = () => {
    // Mock find new matches - just show a toast
    toast.success('Found 3 new matches!');
  };

  const handleSignOut = async () => {
    await signOut();
    navigate('/');
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <RefreshCw className="w-8 h-8 animate-spin mx-auto mb-4" />
          <p>Loading your dashboard...</p>
        </div>
      </div>
    );
  }

  const appliedMatches = matches.filter(match => match.application_status === 'pending' || match.application_status === 'applied');
  const shortlistedMatches = matches.filter(match => match.application_status === 'shortlisted');
  const availableMatches = matches.filter(match => !match.application_status || match.application_status === 'matched');

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary/5 to-success/5">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="container mx-auto px-4 py-4">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-2xl font-bold text-primary">Smart Internship Matcher</h1>
              <p className="text-muted-foreground">
                Welcome back, {profile?.name || user?.email || 'Student'}
              </p>
            </div>
            <div className="flex items-center gap-4">
              <Button variant="outline" size="sm">
                <Bell className="w-4 h-4 mr-2" />
                Notifications ({notifications.length})
              </Button>
              <Button variant="outline" size="sm">
                <Settings className="w-4 h-4 mr-2" />
                Settings
              </Button>
              <Button variant="outline" size="sm" onClick={handleSignOut}>
                <LogOut className="w-4 h-4 mr-2" />
                Sign Out
              </Button>
            </div>
          </div>
        </div>
      </header>

      <div className="container mx-auto px-4 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Profile Sidebar */}
          <div className="lg:col-span-1">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <User className="w-5 h-5" />
                  Your Profile
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {profile ? (
                  <div>
                    <h3 className="font-semibold">{profile.name}</h3>
                    <div className="space-y-2 mt-2 text-sm text-muted-foreground">
                      <div className="flex items-center gap-2">
                        <BookOpen className="w-4 h-4" />
                        <span>{profile.qualification || 'Not specified'}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <MapPin className="w-4 h-4" />
                        <span>{profile.location || 'Not specified'}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Briefcase className="w-4 h-4" />
                        <span>{profile.experience_level || 'Not specified'}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <TrendingUp className="w-4 h-4" />
                        <span>{profile.sector_interest || 'Not specified'}</span>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="text-center p-4 bg-muted/50 rounded-lg">
                    <User className="w-8 h-8 mx-auto mb-2 text-muted-foreground" />
                    <h3 className="font-medium text-sm mb-1">Profile not completed yet</h3>
                    <p className="text-xs text-muted-foreground">Complete your profile to get better matches</p>
                  </div>
                )}
                
                <div>
                  <h4 className="font-medium mb-2">Skills</h4>
                  <div className="flex flex-wrap gap-1">
                    {profile?.skills?.length > 0 ? (
                      profile.skills.map((skill) => (
                        <Badge key={skill} variant="secondary" className="text-xs">
                          {skill}
                        </Badge>
                      ))
                    ) : (
                      <p className="text-xs text-muted-foreground">No skills added yet</p>
                    )}
                  </div>
                </div>

                <Button 
                  onClick={findNewMatches}
                  disabled={matchingLoading}
                  className="w-full"
                >
                  {matchingLoading ? (
                    <>
                      <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                      Finding Matches...
                    </>
                  ) : (
                    <>
                      <Search className="w-4 h-4 mr-2" />
                      Find New Matches
                    </>
                  )}
                </Button>
              </CardContent>
            </Card>

            {/* Quick Stats */}
            <Card className="mt-4">
              <CardHeader>
                <CardTitle className="text-lg">Quick Stats</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Total Matches</span>
                  <span className="font-semibold">{matches.length}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Applications</span>
                  <span className="font-semibold">{appliedMatches.length}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Shortlisted</span>
                  <span className="font-semibold text-green-600">{shortlistedMatches.length}</span>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Main Content */}
          <div className="lg:col-span-3">
            <Tabs defaultValue="matches" className="w-full">
              <TabsList className="grid w-full grid-cols-4">
                <TabsTrigger value="matches">Available Matches</TabsTrigger>
                <TabsTrigger value="applied">Applied ({appliedMatches.length})</TabsTrigger>
                <TabsTrigger value="shortlisted">Shortlisted ({shortlistedMatches.length})</TabsTrigger>
                <TabsTrigger value="notifications">Notifications</TabsTrigger>
              </TabsList>

              <TabsContent value="matches" className="mt-6">
                <div className="space-y-4">
                  <div className="flex justify-between items-center">
                    <h2 className="text-xl font-semibold">Available Internships</h2>
                    <Badge variant="secondary">{availableMatches.length} matches found</Badge>
                  </div>
                  
                  {availableMatches.length === 0 ? (
                    <Card className="p-8 text-center">
                      <Search className="w-12 h-12 mx-auto mb-4 text-muted-foreground" />
                      <h3 className="text-lg font-semibold mb-2">No matches yet</h3>
                      <p className="text-muted-foreground mb-4">
                        Click "Find New Matches" to discover internships that match your profile
                      </p>
                      <Button onClick={findNewMatches} disabled={matchingLoading}>
                        {matchingLoading ? 'Finding Matches...' : 'Find Matches'}
                      </Button>
                    </Card>
                  ) : (
                    <div className="grid grid-cols-1 gap-4">
                      {availableMatches.map((match) => (
                        <Card key={match.id} className="p-6 hover:shadow-lg transition-shadow">
                          <div className="flex justify-between items-start mb-4">
                            <div>
                              <h3 className="font-semibold text-xl mb-2">Internship Match</h3>
                              <p className="text-sm text-muted-foreground">ID: {match.internship_id}</p>
                            </div>
                            <Badge className="text-lg px-3 py-1">{match.match_score}% Match</Badge>
                          </div>
                          
                           <div className="flex flex-wrap gap-2 mb-4">
                            {match.skill_match && (
                              <Badge variant="secondary" className="flex items-center gap-1">
                                ✅ Skill Match {match.skill_match}%
                              </Badge>
                            )}
                            {match.location_match && (
                              <Badge variant="secondary" className="flex items-center gap-1">
                                🌍 Location Match
                              </Badge>
                            )}
                            {match.rural_priority && (
                              <Badge variant="secondary" className="flex items-center gap-1">
                                🌱 Rural Priority
                              </Badge>
                            )}
                            {match.fresher_friendly && (
                              <Badge variant="secondary" className="flex items-center gap-1">
                                🎓 Fresher-Friendly
                              </Badge>
                            )}
                          </div>
                          
                          {/* Match Reasons - Why this match */}
                          {match.match_reasons && match.match_reasons.length > 0 && (
                            <div className="mb-4 p-3 bg-primary/5 rounded-lg border border-primary/20">
                              <h4 className="text-sm font-semibold mb-2 text-primary">Why this match?</h4>
                              <ul className="space-y-1">
                                {match.match_reasons.map((reason: string, idx: number) => (
                                  <li key={idx} className="text-sm flex items-start gap-2">
                                    <span className="text-primary mt-0.5">•</span>
                                    <span>{reason}</span>
                                  </li>
                                ))}
                              </ul>
                            </div>
                          )}
                          
                          <Button className="w-full">View Details & Apply</Button>
                        </Card>
                      ))}
                    </div>
                  )}
                </div>
              </TabsContent>

              <TabsContent value="applied" className="mt-6">
                <div className="space-y-4">
                  <h2 className="text-xl font-semibold">Your Applications</h2>
                  
                  {appliedMatches.length === 0 ? (
                    <Card className="p-8 text-center">
                      <Briefcase className="w-12 h-12 mx-auto mb-4 text-muted-foreground" />
                      <h3 className="text-lg font-semibold mb-2">No applications yet</h3>
                      <p className="text-muted-foreground">
                        Start applying to internships from your matches
                      </p>
                    </Card>
                  ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {appliedMatches.map((match) => (
                        <Card key={match.id} className="p-4">
                          <h3 className="font-semibold">Match Score: {match.match_score}%</h3>
                          <p className="text-sm text-muted-foreground">Status: {match.application_status}</p>
                          <Badge variant="secondary">{match.status}</Badge>
                        </Card>
                      ))}
                    </div>
                  )}
                </div>
              </TabsContent>

              <TabsContent value="shortlisted" className="mt-6">
                <div className="space-y-4">
                  <h2 className="text-xl font-semibold">Shortlisted Applications</h2>
                  
                  {shortlistedMatches.length === 0 ? (
                    <Card className="p-8 text-center">
                      <Star className="w-12 h-12 mx-auto mb-4 text-muted-foreground" />
                      <h3 className="text-lg font-semibold mb-2">No shortlisted applications</h3>
                      <p className="text-muted-foreground">
                        Companies will shortlist your applications here
                      </p>
                    </Card>
                  ) : (
                     <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                       {shortlistedMatches.map((match) => (
                         <Card key={match.id} className="p-4">
                           <h3 className="font-semibold">Match Score: {match.match_score}%</h3>
                           <p className="text-sm text-muted-foreground">Shortlisted Application</p>
                           <div className="flex items-center gap-2 mt-3">
                             <Badge variant="default">Shortlisted</Badge>
                             <Button 
                               size="sm" 
                               variant="outline"
                               onClick={() => {
                                 setSelectedMatchForFeedback(match);
                                 setFeedbackDialogOpen(true);
                               }}
                             >
                               <MessageSquare className="w-4 h-4 mr-1" />
                               Give Feedback
                             </Button>
                           </div>
                         </Card>
                       ))}
                     </div>
                  )}
                </div>
              </TabsContent>

              <TabsContent value="notifications" className="mt-6">
                <div className="space-y-4">
                  <h2 className="text-xl font-semibold">Recent Notifications</h2>
                  
                  {notifications.length === 0 ? (
                    <Card className="p-8 text-center">
                      <Bell className="w-12 h-12 mx-auto mb-4 text-muted-foreground" />
                      <h3 className="text-lg font-semibold mb-2">No notifications</h3>
                      <p className="text-muted-foreground">
                        You'll receive updates about your applications here
                      </p>
                    </Card>
                  ) : (
                    <div className="space-y-3">
                      {notifications.map((notification: any) => (
                        <Card key={notification.id}>
                          <CardContent className="p-4">
                            <div className="flex justify-between items-start">
                              <div>
                                <h4 className="font-medium">{notification.title}</h4>
                                <p className="text-sm text-muted-foreground">{notification.message}</p>
                              </div>
                              <Badge variant={notification.type === 'success' ? 'default' : 'secondary'}>
                                {notification.type}
                              </Badge>
                            </div>
                          </CardContent>
                        </Card>
                      ))}
                    </div>
                  )}
                </div>
              </TabsContent>
            </Tabs>
          </div>
        </div>
      </div>
      
      {/* Feedback Dialog */}
      {selectedMatchForFeedback && (
        <FeedbackDialog
          open={feedbackDialogOpen}
          onOpenChange={setFeedbackDialogOpen}
          matchId={selectedMatchForFeedback.id}
          userType="student"
          recipientName={selectedMatchForFeedback.internship_id}
        />
      )}
    </div>
  );
};

export default StudentDashboard;